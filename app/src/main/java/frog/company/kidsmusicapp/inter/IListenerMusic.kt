package frog.company.kidsmusicapp.inter

import frog.company.kidsmusicapp.model.Music

interface IListenerMusic {
    fun onResultMusic(array : ArrayList<Music>?)
}