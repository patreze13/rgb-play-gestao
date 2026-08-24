package com.patreze.rgbplaygestao

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class Cliente(
    var nome: String,
    var contato: String,
    var dia: Int
)

class MainActivity : AppCompatActivity() {

    private val preto = Color.rgb(8, 8, 8)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(170, 170, 170)

    private val vermelho = Color.rgb(255, 45, 45)
    private val verde = Color.rgb(0, 230, 118)
    private val azul = Color.rgb(40, 120, 255)

    private val clientes = mutableListOf<Cliente>()

    private lateinit var tela: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = preto
        window.navigationBarColor = preto

        carregarClientes()
        mostrarInicio()
    }

    // ============================================================
    // CONFIGURAÇÃO GERAL
    // ============================================================

    private fun configurarTela() {

        tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(preto)
            setPadding(28, 20, 28, 28)
        }

        setContentView(tela)
    }

    private fun titulo(texto: String): TextView {

        return TextView(this).apply {
            text = texto
            setTextColor(branco)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 20)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun subtitulo(texto: String): TextView {

        return TextView(this).apply {
            text = texto
            setTextColor(cinza)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun campo(dica: String): EditText {

        return EditText(this).apply {

            hint = dica
            hintTextColors =
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(130, 130, 130)
                )

            setTextColor(branco)
            textSize = 16f
            singleLine = true

            setPadding(24, 0, 24, 0)

            setBackgroundColor(
                Color.rgb(25, 25, 25)
            )

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                58
            ).apply {
                setMargins(0, 0, 0, 14)
            }
        }
    }

    private fun botao(
        texto: String,
        cor: Int,
        acao: () -> Unit
    ): TextView {

        return TextView(this).apply {

            text = texto
            setTextColor(branco)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER

            setPadding(18, 0, 18, 0)

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(18, 18, 18))
                setStroke(3, cor)
                cornerRadius = 22f
            }

            setOnClickListener {
                acao()
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                68
            ).apply {
                setMargins(0, 0, 0, 14)
            }
        }
    }

    private fun espaco(pixels: Int): Space {

        return Space(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                1,
                pixels
            )
        }
    }

    // ============================================================
    // TELA INICIAL
    // ============================================================

    private fun mostrarInicio() {

        configurarTela()

        tela.addView(espaco(20))

        val logo = TextView(this).apply {
            text = "RGB PLAY"
            setTextColor(branco)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        tela.addView(
            logo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                70
            )
        )

        tela.addView(
            subtitulo("GESTÃO DE CLIENTES")
        )

        // Este peso mantém os botões centralizados
        val areaBotoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        areaBotoes.addView(
            botao("ADICIONAR CLIENTE", vermelho) {
                mostrarAdicionar()
            }
        )

        areaBotoes.addView(
            botao("VER CLIENTES", verde) {
                mostrarClientes()
            }
        )

        areaBotoes.addView(
            botao("PRÓXIMOS VENCIMENTOS", azul) {
                mostrarProximos()
            }
        )

        tela.addView(
            areaBotoes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        tela.addView(
            TextView(this).apply {
                text = "${clientes.size} cliente(s) cadastrado(s)"
                setTextColor(Color.rgb(100, 100, 100))
                textSize = 12f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                40
            )
        )
    }

    // ============================================================
    // ADICIONAR CLIENTE
    // ============================================================

    private fun mostrarAdicionar() {

        configurarTela()

        tela.addView(titulo("ADICIONAR CLIENTE"))
        tela.addView(
            subtitulo("Cadastre as informações do cliente")
        )

        val nome = campo("Nome do cliente")
        val contato = campo("WhatsApp")
        val dia = campo("Dia do vencimento")

        tela.addView(nome)
        tela.addView(contato)
        tela.addView(dia)

        tela.addView(
            espaco(10)
        )

        tela.addView(
            botao("SALVAR CLIENTE", verde) {

                val nomeTexto = nome.text.toString().trim()
                val contatoTexto = contato.text.toString().trim()
                val diaTexto = dia.text.toString().trim()

                if (nomeTexto.isEmpty()) {
                    nome.error = "Digite o nome"
                    return@botao
                }

                if (diaTexto.isEmpty()) {
                    dia.error = "Digite o dia"
                    return@botao
                }

                val numeroDia = diaTexto.toIntOrNull()

                if (numeroDia == null || numeroDia !in 1..31) {
                    dia.error = "Dia inválido"
                    return@botao
                }

                clientes.add(
                    Cliente(
                        nomeTexto,
                        contatoTexto,
                        numeroDia
                    )
                )

                salvarClientes()

                Toast.makeText(
                    this,
                    "Cliente cadastrado",
                    Toast.LENGTH_SHORT
                ).show()

                mostrarInicio()
            }
        )

        tela.addView(
            botao("VOLTAR", vermelho) {
                mostrarInicio()
            }
        )
    }

    // ============================================================
    // CLIENTES
    // ============================================================

    private fun mostrarClientes() {

        configurarTela()

        tela.addView(titulo("CLIENTES"))

        if (clientes.isEmpty()) {

            tela.addView(
                subtitulo("Nenhum cliente cadastrado.")
            )

            tela.addView(
                botao("VOLTAR", vermelho) {
                    mostrarInicio()
                }
            )

            return
        }

        val lista = clientes.sortedBy {
            proximoVencimento(it.dia)
        }

        val scroll = ScrollView(this)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (cliente in lista) {

            val bloco = TextView(this).apply {

                text =
                    "${cliente.nome}\n" +
                    "Vence todo dia ${cliente.dia}"

                setTextColor(branco)
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(22, 12, 22, 12)

                background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.rgb(20, 20, 20))
                        setStroke(
                            1,
                            Color.rgb(55, 55, 55)
                        )
                        cornerRadius = 18f
                    }

                setOnClickListener {
                    mostrarDetalhes(cliente)
                }

                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        72
                    ).apply {
                        setMargins(0, 0, 0, 10)
                    }
            }

            conteudo.addView(bloco)
        }

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        tela.addView(
            botao("VOLTAR", vermelho) {
                mostrarInicio()
            }
        )
    }

    // ============================================================
    // DETALHES
    // ============================================================

    private fun mostrarDetalhes(cliente: Cliente) {

        configurarTela()

        tela.addView(
            titulo(cliente.nome)
        )

        tela.addView(
            subtitulo("INFORMAÇÕES DO CLIENTE")
        )

        val info = TextView(this).apply {

            val contato =
                if (cliente.contato.isBlank())
                    "Não informado"
                else
                    cliente.contato

            text =
                "Nome\n${cliente.nome}\n\n" +
                "WhatsApp\n$contato\n\n" +
                "Vencimento\nTodo dia ${cliente.dia}"

            setTextColor(branco)
            textSize = 16f
            setPadding(24, 24, 24, 24)

            background =
                android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(20, 20, 20))
                    setStroke(
                        2,
                        Color.rgb(45, 45, 45)
                    )
                    cornerRadius = 20f
                }

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 18)
                }
        }

        tela.addView(info)

        if (cliente.contato.isNotBlank()) {

            tela.addView(
                botao("ABRIR WHATSAPP", verde) {

                    abrirWhatsApp(
                        cliente.contato
                    )
                }
            )
        }

        tela.addView(
            botao("VOLTAR", vermelho) {
                mostrarClientes()
            }
        )
    }

    // ============================================================
    // PRÓXIMOS VENCIMENTOS
    // ============================================================

    private fun mostrarProximos() {

        configurarTela()

        tela.addView(
            titulo("PRÓXIMOS VENCIMENTOS")
        )

        val hoje = Calendar.getInstance()

        val proximos = clientes.map {
            val data = proximoVencimento(it.dia)
            val dias = diasEntre(hoje, data)

            Pair(it, dias)
        }.filter {
            it.second in 0..3
        }.sortedBy {
            it.second
        }

        if (proximos.isEmpty()) {

            tela.addView(
                subtitulo(
                    "Nenhum cliente vence nos próximos 3 dias."
                )
            )

        } else {

            val scroll = ScrollView(this)

            val conteudo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            for ((cliente, dias) in proximos) {

                val textoDias = when (dias) {
                    0 -> "VENCE HOJE"
                    1 -> "VENCE AMANHÃ"
                    else -> "Vence em $dias dias"
                }

                val bloco = TextView(this).apply {

                    text =
                        "${cliente.nome}\n" +
                        "$textoDias • Dia ${cliente.dia}"

                    setTextColor(branco)
                    textSize = 15f
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(22, 12, 22, 12)

                    background =
                        android.graphics.drawable.GradientDrawable().apply {
                            setColor(Color.rgb(20, 20, 20))
                            setStroke(
                                2,
                                azul
                            )
                            cornerRadius = 18f
                        }

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            72
                        ).apply {
                            setMargins(0, 0, 0, 10)
                        }
                }

                conteudo.addView(bloco)
            }

            scroll.addView(conteudo)

            tela.addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        tela.addView(
            botao("VOLTAR", vermelho) {
                mostrarInicio()
            }
        )
    }

    // ============================================================
    // DATAS
    // ============================================================

    private fun proximoVencimento(dia: Int): Calendar {

        val hoje = Calendar.getInstance()

        val resultado =
            Calendar.getInstance().apply {
                set(
                    hoje.get(Calendar.YEAR),
                    hoje.get(Calendar.MONTH),
                    1
                )
            }

        val ultimoDia =
            resultado.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        resultado.set(
            Calendar.DAY_OF_MONTH,
            max(1, minOf(dia, ultimoDia))
        )

        if (resultado.before(hoje)) {

            resultado.add(
                Calendar.MONTH,
                1
            )

            val novoUltimoDia =
                resultado.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

            resultado.set(
                Calendar.DAY_OF_MONTH,
                max(1, minOf(dia, novoUltimoDia))
            )
        }

        return resultado
    }

    private fun diasEntre(
        hoje: Calendar,
        data: Calendar
    ): Int {

        val inicio = Calendar.getInstance().apply {
            timeInMillis = hoje.timeInMillis
            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fim = Calendar.getInstance().apply {
            timeInMillis = data.timeInMillis
            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return (
            (fim.timeInMillis - inicio.timeInMillis)
                / (24 * 60 * 60 * 1000)
            ).toInt()
    }

    // ============================================================
    // WHATSAPP
    // ============================================================

    private fun abrirWhatsApp(numero: String) {

        val somenteNumeros =
            numero.filter {
                it.isDigit()
            }

        val numeroFinal =
            if (somenteNumeros.startsWith("55"))
                somenteNumeros
            else
                "55$somenteNumeros"

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://wa.me/$numeroFinal"
                )
            )

            startActivity(intent)

        } catch (erro: Exception) {

            Toast.makeText(
                this,
                "Não foi possível abrir o WhatsApp.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    private fun salvarClientes() {

        val preferencias =
            getSharedPreferences(
                "rgb_play",
                MODE_PRIVATE
            )

        val editor = preferencias.edit()

        editor.clear()

        clientes.forEachIndexed { indice, cliente ->

            editor.putString(
                "nome_$indice",
                cliente.nome
            )

            editor.putString(
                "contato_$indice",
                cliente.contato
            )

            editor.putInt(
                "dia_$indice",
                cliente.dia
            )
        }

        editor.putInt(
            "quantidade",
            clientes.size
        )

        editor.apply()
    }

    private fun carregarClientes() {

        val preferencias =
            getSharedPreferences(
                "rgb_play",
                MODE_PRIVATE
            )

        val quantidade =
            preferencias.getInt(
                "quantidade",
                0
            )

        clientes.clear()

        for (i in 0 until quantidade) {

            val nome =
                preferencias.getString(
                    "nome_$i",
                    ""
                ) ?: ""

            val contato =
                preferencias.getString(
                    "contato_$i",
                    ""
                ) ?: ""

            val dia =
                preferencias.getInt(
                    "dia_$i",
                    1
                )

            if (nome.isNotBlank()) {

                clientes.add(
                    Cliente(
                        nome,
                        contato,
                        dia
                    )
                )
            }
        }
    }
}
