package com.example.evahome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evahome.adapter.SepetAdapter
import com.example.evahome.databinding.FragmentSepetBinding
import com.example.evahome.dataclass.UrunDataClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale

class SepetFragment : Fragment() {
    private lateinit var binding: FragmentSepetBinding
    private lateinit var sepetAdapter: SepetAdapter
    private val sepetUrunList = mutableListOf<UrunDataClass>()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSepetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sepetAdapter = SepetAdapter(requireContext(), sepetUrunList)
        binding.sepetList.adapter = sepetAdapter
        binding.sepetList.layoutManager = LinearLayoutManager(requireContext())

        // Sepet verilerini Firebase'den al
        verileriAl()

        // Sepeti temizle butonuna tıklanıldığında
        binding.btnSepetiTemizle.setOnClickListener {
            sepetiTemizle()
        }
        binding.btnSiparisVer.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid != null) {
                val sepetRef = FirebaseDatabase.getInstance().reference.child("Kullanicilar").child(uid).child("Sepet")

                val siparislerRef = FirebaseDatabase.getInstance().reference.child("Siparisler")

                sepetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Sepet düğümündeki verileri "Siparisler" düğümüne ekle
                            siparislerRef.child(uid).setValue(snapshot.value) { databaseError, databaseReference ->
                                if (databaseError == null) {
                                    // Ekleme işlemi başarılı olduysa "Sepet" düğümündeki verileri sil
                                    sepetRef.removeValue()
                                } else {
                                    // Hata durumunu ele al
                                    Toast.makeText(context, "Sipariş verilemedi: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Sepet düğümü boşsa kullanıcıya bilgi ver
                            Toast.makeText(context, "Sepetiniz boş. Önce ürün ekleyin.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Hata durumu
                        Toast.makeText(context, "Veritabanı hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }


    }

    fun verileriAl() {
        val kullaniciId = auth.currentUser?.uid
        if (kullaniciId != null) {
            val sepetRef =
                FirebaseDatabase.getInstance().getReference("Kullanicilar/$kullaniciId/Sepet")

            // Sepet verilerini al ve RecyclerView'e ekle
            sepetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sepetUrunList.clear()
                    var toplamFiyat = 0

                    for (sepetSnapshot in snapshot.children) {
                        val urunIsim = sepetSnapshot.child("urunIsim").getValue(String::class.java)
                        val urunId = sepetSnapshot.child("urunId").getValue(String::class.java)
                        val urunAciklama =
                            sepetSnapshot.child("urunAciklama").getValue(String::class.java)
                        val newimageurl =
                            sepetSnapshot.child("newimageurl").getValue(String::class.java)
                        val urunStok =
                            sepetSnapshot.child("urunStok").getValue(Int::class.java)
                        val urunAdet = sepetSnapshot.child("urunAdet").getValue(Int::class.java)
                        val urunFiyat = sepetSnapshot.child("urunFiyat").getValue(Int::class.java)

                        if (urunIsim != null && urunFiyat != null && urunAdet != null) {
                            val sepetUrun = UrunDataClass(
                                urunId,
                                urunIsim,
                                newimageurl!!,
                                urunAciklama,
                                urunFiyat,
                                urunStok,
                                urunAdet
                            )
                            sepetUrunList.add(sepetUrun)
                            toplamFiyat += urunFiyat * urunAdet
                        }
                    }

                    guncelToplamFiyatGoster(toplamFiyat)
                    sepetAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Hata durumu
                }
            })
        }
    }

    private fun guncelToplamFiyatGoster(toplamFiyat: Int) {
        val formattedToplamFiyat = NumberFormat.getCurrencyInstance(Locale("tr", "TR")).format(toplamFiyat)
        binding.toplamFiyat.text = formattedToplamFiyat
    }

    private fun sepetiTemizle() {
        val kullaniciId = auth.currentUser?.uid
        if (kullaniciId != null) {
            val sepetRef =
                FirebaseDatabase.getInstance().getReference("Kullanicilar/$kullaniciId/Sepet")
            sepetRef.removeValue()
            guncelToplamFiyatGoster(0)
        }
    }
}
