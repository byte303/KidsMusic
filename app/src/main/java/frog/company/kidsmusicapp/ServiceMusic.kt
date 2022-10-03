package frog.company.kidsmusicapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import frog.company.kidsmusicapp.db.UtilsDatabase
import frog.company.kidsmusicapp.inter.IListenerMusic
import frog.company.kidsmusicapp.model.Music
import kotlinx.coroutines.*

class ServiceMusic : Service(), IListenerMusic, CoroutineScope {

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    interface MusicClient{
        fun serviceArray(arrays : ArrayList<Music>)
        fun serviceSelect(music : Music)
        fun serviceStop()
        fun servicePlay()
        fun isExiting()
    }

    companion object{
        private lateinit var notificationManager: NotificationManager
        private var builder: Notification.Builder? = null
        var playQueue = ArrayList<Music>()
        var mediaPlayer = MediaPlayer()
        var numberTrack = 0
        var checkPlay = false

        val registeredClients = mutableSetOf<MusicClient>()

        fun registerClient(client: Any){
            try {
                registeredClients.add(client as MusicClient)
            } catch(e: ClassCastException){
                Log.e("ERR>", "Could not register client!")
                Log.e("ERR>", e.message.toString())
            }
        }

        fun unregisterClient(client: Any){
            try {
                registeredClients.remove(client as MusicClient)
            } catch(e: ClassCastException){}
        }
    }
    private val binder = MusicBinder(this@ServiceMusic)

    class MusicBinder(private val service: ServiceMusic) : Binder() {
        fun getService(): ServiceMusic {
            return service
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun generateAction(
        icon: Int,
        title: String,
        intentAction: String
    ): Notification.Action {
        val intent = Intent(this, ServiceMusic::class.java)
        intent.action = intentAction
        val pendingIntent =
            PendingIntent.getService(this, 1, intent, 0)
        @Suppress("DEPRECATION")
        return Notification.Action.Builder(icon, title, pendingIntent).build()
    }

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()

        Log.e("OnData", "Service start")
        UtilsDatabase().MyTask(this).execute()

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (builder == null) {
            builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, "10002")
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }

            val intent = Intent(this, ServiceMusic::class.java)
            intent.action = "ACTION_STOP"

            (builder as Notification.Builder)
                .setSmallIcon(R.drawable.notif_play)
                .setSubText("Аудиоплеер")
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        0
                    )
                )
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setDeleteIntent(PendingIntent.getService(this, 1, intent, 0))
                .setOngoing(true)
        }
    }

    private fun showNotification(
        action: Notification.Action
    ) {
        builder?.setContentTitle(playQueue[numberTrack].title)
        builder?.setContentText(playQueue[numberTrack].text)

        builder?.addAction(
            generateAction(
                R.drawable.notif_previous,
                getString(R.string.prev),
                "ACTION_PREVIOUS"
            )
        )

        builder?.addAction(action)

        builder?.addAction(
            generateAction(
                R.drawable.notif_next,
                getString(R.string.next),
                "ACTION_NEXT"
            )
        )

        builder?.addAction(
            generateAction(
                R.drawable.kill,
                getString(R.string.kill),
                "ACTION_KILL"
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "10002",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel("my_service", "My Background Service")
            builder?.setChannelId(channelId)?.build()
        } else
            builder?.build()

        val notification = builder?.setOngoing(true)!!
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(playQueue[numberTrack].title)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        //unregisterReceiver(receiver)
        //exitProcess(0)
    }

    override fun onResultMusic(array: ArrayList<Music>?) {
        if(array != null){
            playQueue  = array
            launch(Dispatchers.Default) {
                registeredClients.forEach { it.serviceArray(playQueue ) }
            }
        }
    }
    private fun onPlaySound(num: Int, play: Boolean = false) {

        if (checkPlay) onStopSound()

        if (!play) mediaPlayer = MediaPlayer.create(this, Uri.parse(playQueue[num].music))

        showNotification(
            generateAction(
                R.drawable.notif_pause,
                getString(R.string.pause),
                "ACTION_PAUSE"
            )
        )

        mediaPlayer.start()

        checkPlay = true
        numberTrack = num
        onSelect(num)

        mediaPlayer.setOnCompletionListener {
            if (numberTrack == playQueue.size - 1)
                onPlaySound(0)
            else {
                numberTrack += 1
                onPlaySound(numberTrack)
            }
        }
        launch(Dispatchers.Default) {
            registeredClients.forEach { it.servicePlay() }
        }
    }

    private fun onStopSound() {
        mediaPlayer.pause()
        checkPlay = false

        showNotification(
            generateAction(
                R.drawable.notif_play,
                getString(R.string.play),
                "ACTION_PLAY"
            )
        )

        launch(Dispatchers.Default) {
            registeredClients.forEach { it.serviceStop() }
        }
    }

    private fun onSelect(num: Int) {
        launch(Dispatchers.Default) {
            registeredClients.forEach { it.serviceSelect(playQueue[num]) }
        }
    }

    fun onStartStop(){
        if (!checkPlay) onPlaySound(numberTrack, true)
        else onStopSound()
    }

    fun onBack(){
        if (numberTrack == 0)
            onPlaySound(playQueue.size - 1)
        else {
            numberTrack--
            onPlaySound(numberTrack)
        }
    }

    fun onNext(){
        if (numberTrack == playQueue.size - 1)
            onPlaySound(0)
        else {
            numberTrack++
            onPlaySound(numberTrack)
        }
    }

    fun onStatus() = checkPlay

    fun onChange(){
        launch(Dispatchers.Default) {
            Log.e("ServiceMusic", "OnChange")
            registeredClients.forEach { it.serviceSelect(playQueue[numberTrack]) }
        }
    }

    fun onSet(index : Int){
        numberTrack = index
        onPlaySound(numberTrack)
    }

    fun onGetNumberTrack() = numberTrack

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return
        val action = intent.action
        when {
            action.equals("ACTION_PLAY", ignoreCase = true) -> {
                onStartStop()
            }
            action.equals("ACTION_PAUSE", ignoreCase = true) -> {
                onStartStop()
            }
            action.equals("ACTION_PREVIOUS", ignoreCase = true) -> {
                onBack()
            }
            action.equals("ACTION_NEXT", ignoreCase = true) -> {
                onNext()
            }
            action.equals("ACTION_KILL", ignoreCase = true) -> {
                cleanUp()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun cleanUp() {
        launch(Dispatchers.Default) {
            registeredClients.forEach(MusicClient::isExiting)
        }
        mediaPlayer.stop()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also {
            it.cancel(1)
        }
    }
}