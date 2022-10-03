package frog.company.kidsmusicapp.db

import android.os.AsyncTask
import android.util.Log
import frog.company.kidsmusicapp.inter.IListenerMusic
import frog.company.kidsmusicapp.model.Music
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class UtilsDatabase {

    inner class MyTask(private val listener : IListenerMusic) : AsyncTask<Void?, Void?, Void?>() {

        val musics = ArrayList<Music>()
        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            listener.onResultMusic(musics)
        }
        override fun doInBackground(vararg p0: Void?): Void? {
            val doc : Document
            try {
                doc = Jsoup.connect("https://firebasestorage.googleapis.com/v0/b/n71inc-music.appspot.com/o/music-site.html?alt=media&token=5d02fdd2-29d5-44cd-9f95-2d5884520b7e").get()
                val html = doc.select("body").text()
                Log.e("onData", html)
                val obj = JSONArray(html)

                if(obj.length() > 0){
                    var data : JSONObject
                    var music : Music

                    for (i in 0 until obj.length()){
                        data = obj.getJSONObject(i)
                        music =
                            Music(
                                data.getInt("id"),
                                data.getString("icon"),
                                data.getString("music"),
                                data.getString("title"),
                                data.getString("text"),
                            )
                        musics.add(music)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}