package com.example.showprofileactivity.profile

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.showprofileactivity.Message
import com.example.showprofileactivity.MessageAdapter
import com.example.showprofileactivity.R
import com.example.showprofileactivity.User
import com.example.showprofileactivity.chat.ChatViewModel
import com.example.showprofileactivity.offers.placeholder.Chat
import com.example.showprofileactivity.offers.placeholder.Offer
import com.example.showprofileactivity.services.ServiceViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val db: FirebaseFirestore
    private val vm  by activityViewModels<ChatViewModel>()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    lateinit var oid:String //offer's id
    lateinit var user:String    //email of the user currently using the app
    lateinit var otherUser:String    //email of the user interested in the offer (not the creator)
    lateinit var creator:String //email of the creator of the related offer
    lateinit var uName:String   //name of the person the user is messaging with
    private var mainMenu: Menu? = null

    init {
        db =  FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        oid = requireArguments().getString("oid").toString()
        user =(requireActivity()!!.intent.extras!!.get("user") as Bundle).getString("email") as String
        creator = requireArguments().getString("cMail").toString()
        val c = creator == user
        mainMenu?.findItem(R.id.accept_request)?.isVisible = c
        mainMenu?.findItem(R.id.reject_request)?.isVisible = c

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        uName = requireArguments().getString("uName").toString()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = uName

        db.collection("chats").document("$oid"+"_"+"$user")
            .addSnapshotListener{ r, e ->
                if(e!=null)
                    _messages.value= emptyList()
                else if(r?.data?.get("messages")!=null)
                    _messages.value = r.toObject(Chat::class.java)!!.messages
                else
                    _messages.value = emptyList()
                setViewModel()
                requireView().findViewById<ProgressBar>(R.id.chatProgressBar).visibility = View.GONE
                requireView().findViewById<RecyclerView>(R.id.rv_chat).visibility = View.VISIBLE
            }
        _messages.value = emptyList()
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = requireView().findViewById<RecyclerView>(R.id.rv_chat)
        rv.layoutManager = LinearLayoutManager(context)


        vm.messages.observe(viewLifecycleOwner) { mList ->
            var adapter = MessageAdapter(mList, user)
            rv.adapter = adapter
            rv.adapter?.notifyDataSetChanged()
            rv.scrollToPosition(mList.size-1)
        }

        val send = requireView().findViewById<Button>(R.id.button_chat_send)
        send.setOnClickListener{
            val text = requireView().findViewById<EditText>(R.id.edit_chat_message).text.toString()
            requireView().findViewById<EditText>(R.id.edit_chat_message).text.clear()
            val currentDate = SimpleDateFormat("dd/MM/yyyy").format(Date())
            val currentTime = SimpleDateFormat("hh:mm").format(Date())
            var c = creator == user
            var m = Message(text, user, c, currentTime, currentDate)
            var t1 = messages.value?.toMutableList()
            t1?.add(m)

            db.collection("chats")
                .document("$oid"+"_"+"$user")
                .set(mapOf("messages" to t1))
                .addOnSuccessListener { Log.d("Firebase", "Chat successfully updated."); rv.adapter?.notifyDataSetChanged() }
                .addOnFailureListener{ Log.d("Firebase", "Failed to update chat.") }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mainMenu = menu
        inflater.inflate(R.menu.chat_menu, menu)
        mainMenu?.findItem(R.id.accept_request)?.isVisible = false
        mainMenu?.findItem(R.id.reject_request)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.accept_request -> {
                if(!requireArguments().getString("accepted").toBoolean()){
                    db.collection("users").document(user).get()
                        .addOnSuccessListener { res ->
                            val accUser = res.toUser()!!
                            val credit = requireArguments().getString("hours")!!.toLong()
                            if(accUser.credit!! < credit){
                                Toast
                                    .makeText(context, "Error: $uName has insufficient credit", Toast.LENGTH_LONG)
                                    .show()
                            }
                            else {
                                db.collection("users").document(otherUser).get()
                                    .addOnSuccessListener {
                                            res ->
                                        val u1 = res.toUser()!!
                                        db.collection("users").document(otherUser).update(mapOf(
                                            "credit" to ((u1.credit as Long) - credit)
                                        ))
                                            .addOnSuccessListener {
                                                db.collection("users").document(user).get()
                                                    .addOnSuccessListener {
                                                            res ->
                                                        val u2 = res.toUser()!!
                                                        db.collection("users").document(user).update(mapOf(
                                                            "credit" to ((u2.credit as Long) + credit)
                                                            ))
                                                            .addOnSuccessListener {
                                                                Toast
                                                                    .makeText(context, "Credit transfered successfully!", Toast.LENGTH_LONG)
                                                                    .show()
                                                            }
                                                            .addOnFailureListener{
                                                                Toast
                                                                    .makeText(context, "Error: can't transfer credit", Toast.LENGTH_LONG)
                                                                    .show()
                                                            }
                                                    }
                                                    .addOnFailureListener{
                                                        Toast
                                                            .makeText(context, "Error: can't retreive needed info", Toast.LENGTH_LONG)
                                                            .show()
                                                    }
                                        }
                                            .addOnFailureListener{
                                                Toast
                                                    .makeText(context, "Error: can't transfer credit", Toast.LENGTH_LONG)
                                                    .show()
                                            }
                                    }
                                    .addOnFailureListener{
                                        Toast
                                            .makeText(context, "Error: can't find the other user", Toast.LENGTH_LONG)
                                            .show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast
                                .makeText(context, "Error", Toast.LENGTH_LONG)
                                .show()
                        }
                }
                else {
                    Toast
                        .makeText(context, "Error: the offer has already been accepted", Toast.LENGTH_LONG)
                        .show()
                }
                findNavController().navigate(R.id.action_toEditProfileFragment)
                true
            }
            R.id.reject_request ->
            {
                findNavController().navigate(R.id.action_showProfileFragment_to_fragment_chat)
                true
            }
            else -> {super.onContextItemSelected(item)}
        }
    }

    fun DocumentSnapshot.toUser(): User? {
        return try{

            val fullname = get("fullname") as String
            val username = get("username") as String?
            val location = get("location") as String?
            val services = get("services") as String?
            val description = get("description") as String?
            val credit = get("credit") as Long
            User(fullname, username, location, services, description, credit)
        } catch(e:Exception){
            e.printStackTrace()
            null
        }
    }

    private fun setViewModel(){
        vm.saveMessages(_messages.value!!)
    }

    private fun DocumentSnapshot.toMessage(): Message? {
        return try {
            val text = get("text") as String
            val user = get("user") as String
            val creator = get("creator") as Boolean
            val time = get("time") as String
            val day = get("day") as String
            Message(text, user, creator, time, day)
        }
        catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

}