package com.lia.ai

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lia.ai.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = ChatAdapter()
    private lateinit var repo: GeminiRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = GeminiRepository { SecurePrefs.get(this).getString(SecurePrefs.KEY_API, null) }

        binding.recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerChat.adapter = adapter

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnSend.setOnClickListener { handleInput() }
        binding.etMessage.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { handleInput(); true } else false
        }

        if (savedInstanceState == null) {
            addLia(
                "Hii! Main Lia hoon 💕 Tumse baat karke accha lagta hai.\n\n" +
                "Commands:\n• play <song> — YouTube par gaana\n• open <package.name> — koi bhi app\n" +
                "• back / home / click <text> — screen control (Accessibility on karna hoga)"
            )
        }
    }

    private fun handleInput() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        addUser(text)

        if (tryLocalCommand(text)) return

        adapter.add(Message("Lia typing...", false))
        scrollToEnd()
        binding.btnSend.isEnabled = false

        lifecycleScope.launch {
            val result = repo.sendMessage(text)
            adapter.removeLast()
            result
                .onSuccess { addLia(it) }
                .onFailure { addLia("Oops 😔 ${it.message}") }
            binding.btnSend.isEnabled = true
        }
    }

    // ---- Local commands -------------------------------------------------

    private fun tryLocalCommand(input: String): Boolean {
        val lower = input.lowercase()
        when {
            lower.startsWith("play ") -> {
                val q = input.substring(5).trim()
                if (q.isNotEmpty()) { openYouTubeMusic(q); return true }
            }
            lower.startsWith("open ") -> {
                val pkg = input.substring(5).trim()
                if (pkg.isNotEmpty()) { openAppByPackage(pkg); return true }
            }
            lower == "back" -> { runAccessibility { it.goBack() }; return true }
            lower == "home" -> { runAccessibility { it.goHome() }; return true }
            lower.startsWith("click ") -> {
                val label = input.substring(6).trim()
                runAccessibility {
                    val ok = it.clickByText(label)
                    addLia(if (ok) "Done ✅" else "'$label' screen par nahi mila.")
                }
                return true
            }
        }
        return false
    }

    private fun openYouTubeMusic(query: String) {
        addLia("Ye lo, '$query' chala rahi hoon 🎶")
        val appIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
            )
            try { startActivity(web) } catch (_: ActivityNotFoundException) {
                addLia("YouTube ya browser nahi mila 😕")
            }
        }
    }

    private fun openAppByPackage(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            addLia("Opening $pkg 🚀")
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            addLia("'$pkg' naam ka koi app installed nahi hai. Package name check karo.")
        }
    }

    private fun runAccessibility(block: (LiaAccessibilityService) -> Unit) {
        val svc = LiaAccessibilityService.instance
        if (svc == null) {
            addLia("Pehle Settings me Accessibility Service enable karo 🙏")
        } else block(svc)
    }

    // ---- UI helpers -----------------------------------------------------

    private fun addUser(t: String) { adapter.add(Message(t, true)); scrollToEnd() }
    private fun addLia(t: String) { adapter.add(Message(t, false)); scrollToEnd() }
    private fun scrollToEnd() {
        binding.recyclerChat.scrollToPosition(adapter.itemCount - 1)
    }
}
