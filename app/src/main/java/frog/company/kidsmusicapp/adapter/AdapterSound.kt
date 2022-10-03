package frog.company.kidsmusicapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import frog.company.kidsmusicapp.R
import frog.company.kidsmusicapp.inter.IListenerClick
import frog.company.kidsmusicapp.model.Music

class AdapterSound(
    private val array : ArrayList<Music>,
    private val listener : IListenerClick
)  :
    RecyclerView.Adapter<AdapterSound.ViewHolder>(){

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtNameSound.text = array[position].title
        Picasso.get().load(array[position].icon).into(holder.imgPhoto);
        holder.linear.setOnClickListener{
            listener.onClickIndex(position)
        }
    }

    override fun getItemCount(): Int {
        return array.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_sound,
                parent,
                false
            )
        )
    }
    inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view){
        var imgPhoto : ImageView = view.findViewById(R.id.imgPhoto)
        var txtNameSound : TextView = view.findViewById(R.id.txtNameSound)
        var linear : RelativeLayout = view.findViewById(R.id.main_linear)

    }
}