package frog.company.kidsmusicapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import frog.company.kidsmusicapp.databinding.ActivityMainBinding
import frog.company.kidsmusicapp.model.Music
import frog.company.kidsmusicapp.ui.HomeFragment
import frog.company.kidsmusicapp.utils.Shared
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), ServiceMusic.MusicClient, CoroutineScope {

    private var mService: ServiceMusic? = null
    private lateinit var serviceConn: ServiceConnection

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().replace(
            R.id.viewPager,
            HomeFragment()
        ).commit()

        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService = (service as ServiceMusic.MusicBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }

        configureBannerView()
    }

    private fun bindService() {
        if (Shared.serviceRunning(ServiceMusic::class.java, this@MainActivity))
            bindService(
                Intent(this@MainActivity, ServiceMusic::class.java),
                serviceConn,
                0
            )
    }

    private fun configureBannerView() {
        MobileAds.initialize(this) {}

        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-8656092455522046/3167168630"

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.Default) {
            ServiceMusic.registerClient(this)
        }
        if(mService == null)
            bindService()
    }

    override fun onPause() {
        super.onPause()
        launch(Dispatchers.Default) {
            ServiceMusic.unregisterClient(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        ServiceMusic.unregisterClient(this)
    }

    override fun serviceArray(arrays: ArrayList<Music>) {

    }

    override fun serviceSelect(music: Music) {

    }

    override fun serviceStop() {

    }

    override fun servicePlay() {

    }

    override fun isExiting() {

    }
}