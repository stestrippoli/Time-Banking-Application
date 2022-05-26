package com.example.showprofileactivity.profile

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.showprofileactivity.R
import com.example.showprofileactivity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {

    private val profileViewModel : ProfileViewModel by activityViewModels()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var email: String
    var user: User? = null
    private var mainMenu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var editmode: Boolean
        setHasOptionsMenu(true)
        if(arguments==null) {
            email = FirebaseAuth.getInstance().currentUser?.email!!
            editmode = true
        }
        else{
            email = requireArguments().getString("email").toString()
            editmode = false
        }
        db.collection("users").document(email).get()
            .addOnSuccessListener { res ->
                user = res.toUser()!!
                setViewModel()
                mainMenu?.findItem(R.id.modifybtn)?.isVisible = editmode
                mainMenu?.findItem(R.id.to_chat)?.isVisible = !editmode
                requireView().findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                requireView().findViewById<ConstraintLayout>(R.id.profileLayout).visibility = View.VISIBLE
            }
            .addOnFailureListener {
                Toast
                    .makeText(context, "Error", Toast.LENGTH_LONG)
                    .show()
            }

        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.fullname.observe(viewLifecycleOwner) { fullname ->
            requireView().findViewById<TextView>(R.id.fullname).text = fullname
        }
        profileViewModel.nickname.observe(viewLifecycleOwner) { nickname ->
            requireView().findViewById<TextView>(R.id.nickname).text = nickname
        }
        profileViewModel.email.observe(viewLifecycleOwner) { email ->
            requireView().findViewById<TextView>(R.id.email).text = email
        }
        profileViewModel.location.observe(viewLifecycleOwner) { location ->
            requireView().findViewById<TextView>(R.id.location).text = location
        }
        profileViewModel.skills.observe(viewLifecycleOwner) { skills ->
            requireView().findViewById<TextView>(R.id.skills).text = skills
        }
        profileViewModel.description.observe(viewLifecycleOwner) { description ->
            requireView().findViewById<TextView>(R.id.description).text = description
        }
        profileViewModel.picture.observe(viewLifecycleOwner) { picture ->
            requireView().findViewById<ImageView>(R.id.profilepic).setImageURI(picture.toString().toUri())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mainMenu = menu
        inflater.inflate(R.menu.custom_menu, menu)
        mainMenu?.findItem(R.id.modifybtn)?.isVisible = false
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.modifybtn -> {
                findNavController().navigate(R.id.action_toEditProfileFragment)
                true
            }
            R.id.to_chat ->
            {
                findNavController().navigate(R.id.action_showProfileFragment_to_fragment_chat)
                true
            }
            else -> {super.onContextItemSelected(item)}
        }
    }

    private fun setViewModel(){
       val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val myJSON = JSONObject(
            sharedPref.getString("profile",
                """{"fullname":"Default Name","nickname":"Default nickname","email":"default@email.com","location":"Default location","skills":"Skill1 | Skill2 | Skill3","description": "Default description","img": "android.resource://com.example.showprofileactivity/${R.drawable.propic}"}""")
        )

        /*sharedViewModel.saveFullname(myJSON.getString("fullname").toString())
        sharedViewModel.saveNickname(myJSON.getString("nickname").toString())
        sharedViewModel.saveEmail(myJSON.getString("email").toString())
        sharedViewModel.saveLocation(myJSON.getString("location").toString())
        sharedViewModel.saveSkills(myJSON.getString("skills").toString())
        sharedViewModel.saveDescription(myJSON.getString("description").toString())
        sharedViewModel.savePicture( myJSON.getString("img").toString())*/

        val (fullname, username, location, services, description) = user!!

        profileViewModel.saveFullname(fullname)
        profileViewModel.saveNickname(username?:"Your Username" as String)
        profileViewModel.saveLocation(location?:"Your Location" as String)
        profileViewModel.saveSkills(services?:"" as String)
        profileViewModel.saveDescription(description?:"Your Description" as String)
        profileViewModel.saveEmail(email)
        if(myJSON.has("img"))
            profileViewModel.savePicture(myJSON.getString("img"))
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

}