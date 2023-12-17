package com.example.evahome.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.evahome.databinding.RvUserBinding
import com.example.evahome.dataclass.KullaniciBilgi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class KullaniciAdapter(
    private val context: Context,
    private val kullaniciListesi: List<KullaniciBilgi>
) :
    RecyclerView.Adapter<KullaniciAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: RvUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // ViewHolder içinde bir View referansı tanımlayın

        fun bind(kullanici: KullaniciBilgi) {
            binding.KullaniciEmail.text = kullanici.email



            binding.userSil.setOnClickListener {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                // Kullanıcının UID'sini al
                val selectedUserUid = kullanici.userId

                // Eğer silinmeye çalışılan kullanıcı, şu an oturum açık olan kullanıcı değilse devam et
                if (currentUser?.uid != selectedUserUid) {
                    // Kullanıcıyı silme işlemlerini gerçekleştir
                    kullanici.email?.let { it1 ->
                        kullanici.sifre?.let { it2 ->
                            auth.signInWithEmailAndPassword(it1, it2)
                                .addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        // Oturum açıldıktan sonra kullanıcıyı sil
                                        currentUser?.delete()
                                            ?.addOnCompleteListener { deleteTask ->
                                                if (deleteTask.isSuccessful) {
                                                    // Kullanıcı başarıyla silindi
                                                    // Diğer işlemleri gerçekleştir

                                                    // Firebase Realtime Database'den de sil
                                                    val databaseReference =
                                                        selectedUserUid?.let { it1 ->
                                                            FirebaseDatabase.getInstance().reference.child(
                                                                "Kullanicilar"
                                                            )
                                                                .child(it1)
                                                        }

                                                    if (databaseReference != null) {
                                                        databaseReference.removeValue()
                                                            .addOnCompleteListener { databaseTask ->
                                                                if (databaseTask.isSuccessful) {
                                                                    // Realtime Database'den de başarıyla silindi
                                                                } else {
                                                                    Log.d(
                                                                        "KullaniciListeAdapter",
                                                                        "Database silme hatası: ${databaseTask.exception?.message}"
                                                                    )
                                                                }
                                                            }
                                                    }
                                                } else {
                                                    Log.d(
                                                        "KullaniciListeAdapter",
                                                        "Kullanıcı silme hatası: ${deleteTask.exception?.message}"
                                                    )
                                                }
                                            }
                                    } else {
                                        Log.d(
                                            "KullaniciListeAdapter",
                                            "Oturum açma hatası: ${signInTask.exception?.message}"
                                        )
                                    }
                                }
                        }
                    }
                } else {
                    // Şu anki oturum açık olan kullanıcıyı silmek isteme hatası
                    Log.d(
                        "KullaniciListeAdapter",
                        "Şu anki oturum açık olan kullanıcıyı silmek isteme hatası"
                    )
                }


            }


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvUserBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(kullaniciListesi[position])


    }

    override fun getItemCount(): Int {
        return kullaniciListesi.size
    }


}

