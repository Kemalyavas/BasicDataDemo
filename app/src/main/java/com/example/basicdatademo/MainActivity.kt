package com.example.basicdatademo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// "settings" isimli DataStore tanımla
val Context.settingsDataStore by preferencesDataStore(name = "settings")

// "user_note.pb" isimli Proto DataStore — UserNote tipinde, schema'lı
val Context.userNoteDataStore: DataStore<UserNote> by dataStore(
    fileName = "user_note.pb",
    serializer = UserNoteSerializer
)

class MainActivity : AppCompatActivity() {

    // Profil verileri için SharedPreferences
    private lateinit var prefs: SharedPreferences

    // DataStore key'leri — tip belirtiliyor, yanlış tipte okuma imkansız
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        val VISIT_COUNT_KEY = intPreferencesKey("visit_count")
    }

    // Ekran elemanları
    private lateinit var etUsername: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var switchDarkMode: MaterialSwitch
    private lateinit var switchNotifications: MaterialSwitch
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var tvVisitCount: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var etNote: TextInputEditText
    private lateinit var tvNoteSaved: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // SharedPreferences dosyasını al, MODE_PRIVATE = sadece bu uygulama erişir
        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Dark mode cache — DataStore asenkron olduğu için temayı hızlı uygulamak için
        val isDark = prefs.getBoolean("dark_mode_cache", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // XML elemanlarını bağla
        etUsername = findViewById(R.id.etUsername)
        etAge = findViewById(R.id.etAge)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        btnSave = findViewById(R.id.btnSave)
        btnClear = findViewById(R.id.btnClear)
        tvVisitCount = findViewById(R.id.tvVisitCount)
        tvWelcome = findViewById(R.id.tvWelcome)
        etNote = findViewById(R.id.etNote)
        tvNoteSaved = findViewById(R.id.tvNoteSaved)

        // Sayacı artır — coroutine içinde çünkü DataStore asenkron
        lifecycleScope.launch {
            incrementVisitCount()
        }

        // Kayıtlı verileri yükle
        loadData()

        btnSave.setOnClickListener { saveData() }
        btnClear.setOnClickListener { clearData() }

        // Dark mode değişince → DataStore'a yaz + temayı değiştir
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsDataStore.edit { it[DARK_MODE_KEY] = isChecked }
                prefs.edit().putBoolean("dark_mode_cache", isChecked).apply()
            }
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Bildirim değişince → DataStore'a yaz
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsDataStore.edit { it[NOTIFICATIONS_KEY] = isChecked }
            }
        }
    }

    // Her açılışta sayacı 1 artır — DataStore ile
    private suspend fun incrementVisitCount() {
        settingsDataStore.edit { settings ->
            val current = settings[VISIT_COUNT_KEY] ?: 0  // yoksa 0
            settings[VISIT_COUNT_KEY] = current + 1
        }
        val count = settingsDataStore.data.first()[VISIT_COUNT_KEY] ?: 1
        runOnUiThread {
            tvVisitCount.text = "Bu uygulamayı $count. kez açıyorsunuz"
        }
    }

    // Profil → SharedPreferences'tan, Ayarlar → DataStore'dan oku
    private fun loadData() {
        val username = prefs.getString("username", "") ?: ""
        val age = prefs.getString("age", "") ?: ""
        etUsername.setText(username)
        etAge.setText(age)

        tvWelcome.text = if (username.isNotEmpty()) "Hoş geldin, $username!" else "Basic Data Demo"

        // DataStore asenkron olduğu için coroutine içinde
        lifecycleScope.launch {
            val settings = settingsDataStore.data.first()
            val isDark = settings[DARK_MODE_KEY] ?: false
            val notif = settings[NOTIFICATIONS_KEY] ?: true
            runOnUiThread {
                switchDarkMode.isChecked = isDark
                switchNotifications.isChecked = notif
            }
        }

        // Notu Proto DataStore'dan oku
        lifecycleScope.launch {
            val note = userNoteDataStore.data.first()
            runOnUiThread {
                etNote.setText(note.noteText)
                tvNoteSaved.text = formatSavedAt(note.savedAt)
            }
        }
    }

    // Profili SharedPreferences'a kaydet — edit, putString, apply
    private fun saveData() {
        val username = etUsername.text.toString()
        val age = etAge.text.toString()

        prefs.edit()
            .putString("username", username)
            .putString("age", age)
            .apply()

        if (username.isNotEmpty()) {
            tvWelcome.text = "Hoş geldin, $username!"
        }

        // Notu Proto DataStore'a yaz — typed, schema'lı
        val noteText = etNote.text.toString()
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            userNoteDataStore.updateData {
                it.toBuilder().setNoteText(noteText).setSavedAt(now).build()
            }
            runOnUiThread { tvNoteSaved.text = formatSavedAt(now) }
        }

        Snackbar.make(
            findViewById(android.R.id.content),
            "Profil → SharedPreferences  |  Ayarlar → DataStore  |  Not → Proto DataStore",
            Snackbar.LENGTH_LONG
        ).show()
    }

    // Üçünü de temizle
    private fun clearData() {
        prefs.edit().clear().apply()                          // SharedPreferences sil
        lifecycleScope.launch { settingsDataStore.edit { it.clear() } }  // DataStore sil
        // Proto DataStore'u boş UserNote ile sıfırla
        lifecycleScope.launch {
            userNoteDataStore.updateData { UserNote.getDefaultInstance() }
        }

        etUsername.setText("")
        etAge.setText("")
        switchDarkMode.isChecked = false
        switchNotifications.isChecked = true
        etNote.setText("")
        tvNoteSaved.text = "Henüz kaydedilmedi"
        tvWelcome.text = "Basic Data Demo"
        tvVisitCount.text = "Veriler sıfırlandı"

        Snackbar.make(
            findViewById(android.R.id.content),
            "Tüm veriler silindi!",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    // savedAt → "Son kayıt: 08 May 2026 14:32" / boşsa "Henüz kaydedilmedi"
    private fun formatSavedAt(savedAt: Long): String {
        if (savedAt == 0L) return "Henüz kaydedilmedi"
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))
        return "Son kayıt: ${sdf.format(Date(savedAt))}"
    }
}