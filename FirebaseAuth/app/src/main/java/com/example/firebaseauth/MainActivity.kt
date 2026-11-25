
package com.example.firebaseauth

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText

    private lateinit var btnLogin: Button
    private lateinit var btnCadastrar: Button
    private lateinit var btnRedefinir: Button
    private lateinit var btnExcluir: Button
    private lateinit var btnSair: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)

        btnLogin = findViewById(R.id.btnLogin)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        btnRedefinir = findViewById(R.id.btnRedefinir)
        btnExcluir = findViewById(R.id.btnExcluir)
        btnSair = findViewById(R.id.btnSair)

        btnLogin.setOnClickListener { login() }
        btnCadastrar.setOnClickListener { cadastrar() }
        btnRedefinir.setOnClickListener { redefinirSenha() }
        btnExcluir.setOnClickListener { confirmarExclusao() }

        btnSair.setOnClickListener {
            auth.signOut()
            atualizarBotoes()
            mensagem("Você saiu da conta.")
        }

        atualizarBotoes()
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        if (!validarDados(email, senha)) return

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mensagem("Login realizado com sucesso!")
                } else {
                    mensagem(
                        task.exception?.localizedMessage
                            ?: "Erro ao realizar login."
                    )
                }
                atualizarBotoes()
            }
    }

    private fun cadastrar() {
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        if (!validarDados(email, senha)) return

        btnCadastrar.isEnabled = false

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mensagem("Usuário criado com sucesso!")
                } else {
                    mensagem(
                        task.exception?.localizedMessage
                            ?: "Erro ao criar usuário."
                    )
                }
                atualizarBotoes()
            }
    }

    private fun redefinirSenha() {
        val email = etEmail.text.toString().trim()

        if (email.isBlank() ||
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            mensagem("Digite um e-mail válido.")
            return
        }

        btnRedefinir.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mensagem("Verifique seu e-mail para redefinir a senha.")
                } else {
                    mensagem(
                        task.exception?.localizedMessage
                            ?: "Erro ao enviar o e-mail."
                    )
                }
                atualizarBotoes()
            }
    }

    private fun confirmarExclusao() {
        val usuario = auth.currentUser

        if (usuario == null) {
            mensagem("Faça login antes de excluir a conta.")
            atualizarBotoes()
            return
        }

        val campoSenha = EditText(this).apply {
            hint = "Senha atual"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Excluir minha conta")
            .setMessage("Digite sua senha para confirmar a exclusão permanente.")
            .setView(campoSenha)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                val senha = campoSenha.text.toString()
                val email = usuario.email

                if (senha.isBlank() || email.isNullOrBlank()) {
                    mensagem("Informe sua senha atual.")
                    return@setPositiveButton
                }

                val credencial =
                    EmailAuthProvider.getCredential(email, senha)

                usuario.reauthenticate(credencial)
                    .addOnCompleteListener(this) { reauth ->
                        if (reauth.isSuccessful) {
                            usuario.delete()
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        atualizarBotoes()
                                        mensagem("Conta excluída com sucesso!")
                                    } else {
                                        mensagem(
                                            task.exception?.localizedMessage
                                                ?: "Erro ao excluir a conta."
                                        )
                                    }
                                }
                        } else {
                            mensagem("Falha ao confirmar a senha.")
                        }
                    }
            }
            .show()
    }

    private fun validarDados(
        email: String,
        senha: String
    ): Boolean {
        if (email.isBlank() || senha.isBlank()) {
            mensagem("Preencha o e-mail e a senha.")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mensagem("Digite um e-mail válido.")
            return false
        }

        if (senha.length < 6) {
            mensagem("A senha deve ter pelo menos 6 caracteres.")
            return false
        }

        return true
    }

    private fun atualizarBotoes() {
        val logado = auth.currentUser != null

        btnLogin.isEnabled = !logado
        btnCadastrar.isEnabled = !logado
        btnRedefinir.isEnabled = true
        btnExcluir.isEnabled = logado
        btnSair.isEnabled = logado
    }

    private fun mensagem(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }
}
