import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.data.ClassSchedule

class ProfSchedAdapter(private var scheduleList: List<ClassSchedule>) :
    RecyclerView.Adapter<ProfSchedAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubject: TextView = view.findViewById(R.id.tvSubject)
        val tvRoom: TextView = view.findViewById(R.id.tvRoom)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = scheduleList[position]
        holder.tvSubject.text = schedule.subject
        holder.tvRoom.text = "Room: ${schedule.room}"
        holder.tvTime.text = "Time: ${schedule.time}"
    }

    override fun getItemCount(): Int = scheduleList.size

    fun updateList(newList: List<ClassSchedule>) {
        scheduleList = newList
        notifyDataSetChanged()
    }
}
