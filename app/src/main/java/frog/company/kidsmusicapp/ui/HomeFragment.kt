package frog.company.kidsmusicapp.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.squareup.picasso.Picasso
import frog.company.kidsmusicapp.R
import frog.company.kidsmusicapp.ServiceMusic
import frog.company.kidsmusicapp.adapter.AdapterSound
import frog.company.kidsmusicapp.databinding.FragmentHomeBinding
import frog.company.kidsmusicapp.inter.IListenerClick
import frog.company.kidsmusicapp.model.Music
import frog.company.kidsmusicapp.utils.AppConst
import frog.company.kidsmusicapp.utils.Shared
import io.paperdb.Paper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class HomeFragment : Fragment(), ServiceMusic.MusicClient, CoroutineScope, IListenerClick {

    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var mInterstitialAd: InterstitialAd? = null

    override val coroutineContext = Dispatchers.Main + SupervisorJob()
    private lateinit var serviceConn: ServiceConnection
    var isBound = false
    var mService: MutableStateFlow<ServiceMusic?> = MutableStateFlow(null)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        Paper.init(requireContext())

        configureInterstitialAd()

        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService.value = (service as ServiceMusic.MusicBinder).getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                isBound = false
            }
        }

        binding.listSound.layoutManager = LinearLayoutManager(requireContext())

        binding.include.txtNameSound.ellipsize = TextUtils.TruncateAt.MARQUEE
        binding.include.txtNameSound.isSingleLine = true
        binding.include.txtNameSound.marqueeRepeatLimit = -1
        binding.include.txtNameSound.isSelected = true

        binding.include.imgPlay.setOnClickListener {
            launch(Dispatchers.IO) {
                mService.value?.onStartStop()
            }
        }
        binding.include.imgBack.setOnClickListener {
            launch(Dispatchers.IO) {
                mService.value?.onBack()
            }
            loadInterstitialAd()
        }
        binding.include.imgNext.setOnClickListener {
            launch(Dispatchers.IO) {
                mService.value?.onNext()
            }
        }
        bindEvent()
        onStartService()
        return binding.root
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireActivity(), "ca-app-pub-8656092455522046/1554704067", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("Tag", adError.message)
                    mInterstitialAd = null

                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("Tag", "Ad was loaded.")
                    mInterstitialAd = interstitialAd

                    if (mInterstitialAd != null) {
                        mInterstitialAd?.show(requireActivity())
                    }
                }
            }
        )
    }

    private fun configureInterstitialAd() {


    }

    private fun onStartService(){
        if (!Shared.serviceRunning(ServiceMusic::class.java, requireContext())) {
            requireActivity().startService(
                Intent(
                    requireContext(),
                    ServiceMusic::class.java
                )
            )
            bindEvent()
        } else{
            try{
                val arrays = Paper.book().read(AppConst.PAPER_DATA, ArrayList<Music>())!!
                requireActivity().runOnUiThread {
                    binding.listSound.adapter = AdapterSound(arrays, this@HomeFragment)
                }
            } catch (ex : Exception){
                Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindEvent() {
        if (Shared.serviceRunning(ServiceMusic::class.java, requireContext())) {
            try {
                requireContext().bindService(
                    Intent(
                        requireContext(),
                        ServiceMusic::class.java
                    ), serviceConn, 0
                )
                onUpdate(mService.value?.onGetNumberTrack()!!)
            } catch (e: Exception) {
                Log.e("ERR>", e.toString())
            }
        }
    }

    override fun serviceArray(arrays: ArrayList<Music>) {
        launch(Dispatchers.IO) {
            requireActivity().runOnUiThread {
                Paper.book().write(AppConst.PAPER_DATA, arrays)
                binding.listSound.adapter = AdapterSound(arrays, this@HomeFragment)
            }
            mService.value?.onChange()
        }

    }

    private fun onUpdate(index : Int){
        val music = Paper.book().read(AppConst.PAPER_DATA, ArrayList<Music>())!![index]
        binding.include.txtNameSound.text = music.title
        binding.include.txtTextSound.text = music.text

        Picasso.get().load(music.icon).into(binding.include.imgPhoto)
        launch(Dispatchers.IO) {
            if (mService.value?.onStatus() == false)
                binding.include.imgPlay.setImageResource(R.drawable.ic_play)
            else
                binding.include.imgPlay.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun serviceSelect(music: Music) {
        requireActivity().runOnUiThread {
            binding.include.txtNameSound.text = music.title
            binding.include.txtTextSound.text = music.text

            Picasso.get().load(music.icon).into(binding.include.imgPhoto)

            launch(Dispatchers.IO) {
                if (mService.value?.onStatus() == false)
                    binding.include.imgPlay.setImageResource(R.drawable.ic_play)
                else
                    binding.include.imgPlay.setImageResource(R.drawable.ic_pause)
            }
            loadInterstitialAd()
        }
    }

    override fun serviceStop() {
        requireActivity().runOnUiThread {
            binding.include.imgPlay.setImageResource(R.drawable.ic_play)
        }
    }

    override fun servicePlay() {
        requireActivity().runOnUiThread {
            binding.include.imgPlay.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun isExiting() {
        requireActivity().finishAffinity()
    }

    override fun onResume() {
        super.onResume()
        ServiceMusic.registerClient(this)
    }

    override fun onPause() {
        super.onPause()
        ServiceMusic.unregisterClient(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineContext.cancelChildren()
        ServiceMusic.unregisterClient(this)
        _binding = null
    }

    override fun onClickIndex(result: Int) {
        launch(Dispatchers.IO) {
            mService.value?.onSet(result)
        }
    }
}