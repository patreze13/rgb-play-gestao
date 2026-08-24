package com.patreze.rgbplaygestao

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

import org.json.JSONArray
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Cliente(
    var nome: String,
    var contato: String,
    var dia: Int
)

class MainActivity : Activity() {

    private val fundo = Color.rgb(0, 0, 0)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(180, 180, 180)

    private val vermelho = Color.rgb(235, 60, 70)
    private val verde = Color.rgb(50, 210, 100)
    private val azul = Color.rgb(60, 130, 255)

    private val lilas = Color.rgb(190, 150, 255)

    private val preferencias by lazy {
        getSharedPreferences("rgb_play_gestao", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        mostrarInicio()
    }

    // ============================================================
    // DADOS
    // ============================================================

    private fun carregarClientes(): MutableList<Cliente> {

        val lista = mutableListOf<Cliente>()

        val texto = preferencias.getString("clientes", "[]") ?: "[]"

        try {
            val array = JSONArray(texto)

            for (i in 0 until array.length()) {

                val objeto = array.getJSONObject(i)

                lista.add(
                    Cliente(
                        nome = objeto.optString("nome"),
                        contato = objeto.optString("contato"),
                        dia = objeto.optInt("dia")
                    )
                )
            }

        } catch (_: Exception) {
        }

        return lista
    }

    private fun salvarClientes(clientes: List<Cliente>) {

        val array = JSONArray()

        clientes.forEach { cliente ->

            val objeto = JSONObject()

            objeto.put("nome", cliente.nome)
            objeto.put("contato", cliente.contato)
            objeto.put("dia", cliente.dia)

            array.put(objeto)
        }

        preferencias
            .edit()
            .putString("clientes", array.toString())
            .apply()
    }

    // ============================================================
    // VENCIMENTOS
    // ============================================================

    private fun proximoVencimento(dia: Int): Calendar {

        val hoje = Calendar.getInstance()

        val ano = hoje.get(Calendar.YEAR)
        val mes = hoje.get(Calendar.MONTH)

        val tentativa = Calendar.getInstance()

        tentativa.set(
            ano,
            mes,
            1,
            0,
            0,
            0
        )

        tentativa.set(Calendar.MILLISECOND, 0)

        val ultimoDia = tentativa.getActualMaximum(Calendar.DAY_OF_MONTH)

        tentativa.set(
            ano,
            mes,
            minOf(dia, ultimoDia),
            0,
            0,
            0
        )

        tentativa.set(Calendar.MILLISECOND, 0)

        if (!tentativa.before(hoje)) {
            return tentativa
        }

        tentativa.add(Calendar.MONTH, 1)

        val novoUltimoDia =
            tentativa.getActualMaximum(Calendar.DAY_OF_MONTH)

        tentativa.set(
            tentativa.get(Calendar.YEAR),
            tentativa.get(Calendar.MONTH),
            minOf(dia, novoUltimoDia),
            0,
            0,
            0
        )

        tentativa.set(Calendar.MILLISECOND, 0)

        return tentativa
    }

    private fun diasAte(calendario: Calendar): Long {

        val hoje = Calendar.getInstance()

        hoje.set(
            hoje.get(Calendar.YEAR),
            hoje.get(Calendar.MONTH),
            hoje.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )

        hoje.set(Calendar.MILLISECOND, 0)

        val data = calendario.clone() as Calendar

        data.set(
            data.get(Calendar.YEAR),
            data.get(Calendar.MONTH),
            data.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )

        data.set(Calendar.MILLISECOND, 0)

        return (data.timeInMillis - hoje.timeInMillis) /
                (1000L * 60L * 60L * 24L)
    }

    private fun formatarData(calendario: Calendar): String {

        return SimpleDateFormat(
            "dd/MM/yyyy",
            Locale("pt", "BR")
        ).format(calendario.time)
    }

    // ============================================================
    // BASE VISUAL
    // ============================================================

    private fun telaBase(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setBackgroundColor(fundo)

            setPadding(
                28,
                28,
                28,
                28
            )
        }
    }

    private fun titulo(
        texto: String,
        tamanho: Float = 25f
    ): TextView {

        return TextView(this).apply {

            text = texto

            textSize = tamanho

            setTextColor(branco)

            gravity = Gravity.CENTER

            typeface = Typeface.DEFAULT_BOLD

            setPadding(8, 8, 8, 24)
        }
    }

    private fun texto(
        texto: String,
        tamanho: Float = 16f
    ): TextView {

        return TextView(this).apply {

            this.text = texto

            textSize = tamanho

            setTextColor(branco)

            setPadding(
                8,
                8,
                8,
                8
            )
        }
    }

    private fun botao(
        texto: String,
        corBorda: Int,
        acao: () -> Unit
    ): TextView {

        val view = TextView(this)

        view.text = texto
        view.textSize = 16f
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.typeface = Typeface.DEFAULT_BOLD

        view.setPadding(
            16,
            18,
            16,
            18
        )

        view.background = android.graphics.drawable.GradientDrawable().apply {

            setColor(Color.BLACK)

            setStroke(
                3,
                corBorda
            )

            cornerRadius = 18f
        }

        view.setOnClickListener {
            acao()
        }

        return view
    }

    private fun adicionarEspaco(
        container: LinearLayout,
        altura: Int
    ) {

        val espaco = Space(this)

        container.addView(
            espaco,
            LinearLayout.LayoutParams(
                1,
                altura
            )
        )
    }

    // ============================================================
    // TELA INICIAL
    // ============================================================

    private fun mostrarInicio() {

        val tela = telaBase()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(fundo)
        }

        val conteudo = telaBase()

        val logo = TextView(this).apply {

            text = "RGB PLAY"

            textSize = 34f

            setTextColor(branco)

            gravity = Gravity.CENTER

            typeface = Typeface.DEFAULT_BOLD

            setPadding(
                8,
                10,
                8,
                4
            )
        }

        val subtitulo = TextView(this).apply {

            text = "GESTÃO DE CLIENTES"

            textSize = 14f

            setTextColor(cinza)

            gravity = Gravity.CENTER

            setPadding(
                8,
                0,
                8,
                35
            )
        }

        conteudo.addView(
            logo,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        conteudo.addView(
            subtitulo,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        val botaoAdicionar =
            botao(
                "ADICIONAR CLIENTE",
                vermelho
            ) {
                mostrarAdicionarCliente()
            }

        val botaoClientes =
            botao(
                "VER CLIENTES",
                verde
            ) {
                mostrarClientes()
            }

        val botaoVencimentos =
            botao(
                "PRÓXIMOS VENCIMENTOS",
                azul
            ) {
                mostrarProximosVencimentos()
            }

        conteudo.addView(
            botaoAdicionar,
            LinearLayout.LayoutParams(
                -1,
                72
            )
        )

        adicionarEspaco(
            conteudo,
            18
        )

        conteudo.addView(
            botaoClientes,
            LinearLayout.LayoutParams(
                -1,
                72
            )
        )

        adicionarEspaco(
            conteudo,
            18
        )

        conteudo.addView(
            botaoVencimentos,
            LinearLayout.LayoutParams(
                -1,
                72
            )
        )

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(tela)
    }

    // ============================================================
    // CAMPOS
    // ============================================================

    private fun campo(
        dica: String,
        tipo: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {

        return EditText(this).apply {

            hint = dica

            hintTextColor =
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(130, 130, 130)
                )

            setTextColor(branco)

            textSize = 16f

            inputType = tipo

            setSingleLine(true)

            setPadding(
                18,
                10,
                18,
                10
            )

            background =
                android.graphics.drawable.GradientDrawable().apply {

                    setColor(
                        Color.rgb(
                            18,
                            18,
                            18
                        )
                    )

                    setStroke(
                        1,
                        Color.rgb(
                            80,
                            80,
                            80
                        )
                    )

                    cornerRadius = 14f
                }
        }
    }

    // ============================================================
    // ADICIONAR CLIENTE
    // ============================================================

    private fun mostrarAdicionarCliente(
        clienteExistente: Cliente? = null
    ) {

        val editando = clienteExistente != null

        val tela = telaBase()

        val scroll = ScrollView(this)

        val conteudo = telaBase()

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL

        conteudo.addView(
            titulo(
                if (editando)
                    "EDITAR CLIENTE"
                else
                    "ADICIONAR CLIENTE"
            ),
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        val nome = campo(
            "Nome do cliente"
        )

        val contato = campo(
            "Contato / WhatsApp",
            InputType.TYPE_CLASS_PHONE
        )

        val dia = campo(
            "Dia da contratação (1 a 31)",
            InputType.TYPE_CLASS_NUMBER
        )

        if (clienteExistente != null) {

            nome.setText(
                clienteExistente.nome
            )

            contato.setText(
                clienteExistente.contato
            )

            dia.setText(
                clienteExistente.dia.toString()
            )
        }

        conteudo.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                58
            )
        )

        adicionarEspaco(
            conteudo,
            14
        )

        conteudo.addView(
            contato,
            LinearLayout.LayoutParams(
                -1,
                58
            )
        )

        adicionarEspaco(
            conteudo,
            14
        )

        conteudo.addView(
            dia,
            LinearLayout.LayoutParams(
                -1,
                58
            )
        )

        adicionarEspaco(
            conteudo,
            25
        )

        val salvar =
            botao(
                if (editando)
                    "SALVAR ALTERAÇÕES"
                else
                    "CADASTRAR CLIENTE",
                if (editando)
                    azul
                else
                    verde
            ) {

                val nomeTexto =
                    nome.text.toString().trim()

                val contatoTexto =
                    contato.text.toString().trim()

                val diaTexto =
                    dia.text.toString().trim()

                if (nomeTexto.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Informe o nome do cliente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@botao
                }

                val diaNumero =
                    diaTexto.toIntOrNull()

                if (
                    diaNumero == null ||
                    diaNumero !in 1..31
                ) {

                    Toast.makeText(
                        this,
                        "Informe um dia entre 1 e 31.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@botao
                }

                val clientes =
                    carregarClientes()

                if (!editando) {

                    val duplicado =
                        clientes.any {
                            it.nome.equals(
                                nomeTexto,
                                ignoreCase = true
                            )
                        }

                    if (duplicado) {

                        Toast.makeText(
                            this,
                            "Esse cliente já está cadastrado.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@botao
                    }

                    clientes.add(
                        Cliente(
                            nomeTexto,
                            contatoTexto,
                            diaNumero
                        )
                    )

                } else {

                    val antigo =
                        clienteExistente!!

                    val cliente =
                        clientes.find {
                            it.nome == antigo.nome
                        }

                    if (cliente != null) {

                        cliente.nome =
                            nomeTexto

                        cliente.contato =
                            contatoTexto

                        cliente.dia =
                            diaNumero
                    }
                }

                salvarClientes(clientes)

                Toast.makeText(
                    this,
                    if (editando)
                        "Cliente atualizado."
                    else
                        "Cliente cadastrado.",
                    Toast.LENGTH_SHORT
                ).show()

                mostrarInicio()
            }

        conteudo.addView(
            salvar,
            LinearLayout.LayoutParams(
                -1,
                68
            )
        )

        adicionarEspaco(
            conteudo,
            14
        )

        val voltar =
            botao(
                "VOLTAR",
                Color.rgb(
                    100,
                    100,
                    100
                )
            ) {
                mostrarInicio()
            }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(tela)
    }

    // ============================================================
    // LISTA DE CLIENTES
    // ============================================================

    private fun mostrarClientes() {

        val tela = telaBase()

        val scroll = ScrollView(this)

        val conteudo = telaBase()

        conteudo.gravity =
            Gravity.TOP or Gravity.CENTER_HORIZONTAL

        conteudo.addView(
            titulo(
                "CLIENTES",
                24f
            )
        )

        val clientes =
            carregarClientes()

        clientes.sortWith(
            compareBy {
                proximoVencimento(it.dia)
                    .timeInMillis
            }
        )

        if (clientes.isEmpty()) {

            conteudo.addView(
                texto(
                    "Nenhum cliente cadastrado.",
                    17f
                ),
                LinearLayout.LayoutParams(
                    -1,
                    -2
                )
            )

        } else {

            clientes.forEach { cliente ->

                val bloco =
                    criarBlocoCliente(
                        cliente
                    )

                conteudo.addView(
                    bloco,
                    LinearLayout.LayoutParams(
                        -1,
                        72
                    )
                )

                adicionarEspaco(
                    conteudo,
                    10
                )
            }
        }

        val voltar =
            botao(
                "VOLTAR",
                Color.rgb(
                    100,
                    100,
                    100
                )
            ) {
                mostrarInicio()
            }

        adicionarEspaco(
            conteudo,
            20
        )

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(tela)
    }

    private fun criarBlocoCliente(
        cliente: Cliente
    ): TextView {

        val vencimento =
            proximoVencimento(cliente.dia)

        val data =
            SimpleDateFormat(
                "dd/MM",
                Locale("pt", "BR")
            ).format(
                vencimento.time
            )

        return TextView(this).apply {

            text =
                "${cliente.nome}    •    Vence $data"

            textSize = 15f

            setTextColor(branco)

            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                18,
                8,
                18,
                8
            )

            background =
                android.graphics.drawable.GradientDrawable().apply {

                    setColor(
                        Color.rgb(
                            15,
                            15,
                            15
                        )
                    )

                    setStroke(
                        2,
                        verde
                    )

                    cornerRadius = 12f
                }

            setOnClickListener {

                mostrarDetalhesCliente(
                    cliente
                )
            }
        }
    }

    // ============================================================
    // DETALHES
    // ============================================================

    private fun mostrarDetalhesCliente(
        cliente: Cliente
    ) {

        val tela = telaBase()

        val scroll = ScrollView(this)

        val conteudo = telaBase()

        conteudo.gravity =
            Gravity.TOP or Gravity.CENTER_HORIZONTAL

        conteudo.addView(
            titulo(
                cliente.nome,
                24f
            )
        )

        val vencimento =
            proximoVencimento(cliente.dia)

        val dias =
            diasAte(vencimento)

        conteudo.addView(
            texto(
                "Contato\n" +
                        if (cliente.contato.isEmpty())
                            "Não informado"
                        else
                            cliente.contato,
                16f
            )
        )

        conteudo.addView(
            texto(
                "Dia da contratação\n" +
                        "Todo dia ${cliente.dia}",
                16f
            )
        )

        conteudo.addView(
            texto(
                "Próximo vencimento\n" +
                        "${formatarData(vencimento)}\n" +
                        when (dias) {
                            0L -> "Vence hoje"
                            1L -> "Vence amanhã"
                            else -> "Faltam $dias dias"
                        },
                16f
            )
        )

        adicionarEspaco(
            conteudo,
            20
        )

        if (cliente.contato.isNotEmpty()) {

            val whatsapp =
                botao(
                    "ABRIR WHATSAPP",
                    verde
                ) {

                    abrirWhatsApp(
                        cliente.contato
                    )
                }

            conteudo.addView(
                whatsapp,
                LinearLayout.LayoutParams(
                    -1,
                    62
                )
            )

            adicionarEspaco(
                conteudo,
                12
            )
        }

        val editar =
            botao(
                "EDITAR CLIENTE",
                azul
            ) {

                mostrarAdicionarCliente(
                    cliente
                )
            }

        conteudo.addView(
            editar,
            LinearLayout.LayoutParams(
                -1,
                62
            )
        )

        adicionarEspaco(
            conteudo,
            12
        )

        val excluir =
            botao(
                "EXCLUIR CLIENTE",
                vermelho
            ) {

                confirmarExclusao(
                    cliente
                )
            }

        conteudo.addView(
            excluir,
            LinearLayout.LayoutParams(
                -1,
                62
            )
        )

        adicionarEspaco(
            conteudo,
            12
        )

        val voltar =
            botao(
                "VOLTAR",
                Color.rgb(
                    100,
                    100,
                    100
                )
            ) {

                mostrarClientes()
            }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(tela)
    }

    // ============================================================
    // WHATSAPP
    // ============================================================

    private fun abrirWhatsApp(
        contato: String
    ) {

        var numero =
            contato.filter {
                it.isDigit()
            }

        if (numero.length == 10 ||
            numero.length == 11
        ) {

            numero =
                "55$numero"
        }

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://wa.me/$numero"
                    )
                )

            startActivity(intent)

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível abrir o WhatsApp.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // EXCLUSÃO
    // ============================================================

    private fun confirmarExclusao(
        cliente: Cliente
    ) {

        AlertDialog.Builder(this)
            .setTitle("Excluir cliente")
            .setMessage(
                "Deseja realmente excluir ${cliente.nome}?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "EXCLUIR"
            ) { _, _ ->

                val clientes =
                    carregarClientes()

                clientes.removeAll {
                    it.nome == cliente.nome
                }

                salvarClientes(clientes)

                mostrarClientes()
            }
            .show()
    }

    // ============================================================
    // PRÓXIMOS VENCIMENTOS
    // ============================================================

    private fun mostrarProximosVencimentos() {

        val tela = telaBase()

        val scroll = ScrollView(this)

        val conteudo = telaBase()

        conteudo.gravity =
            Gravity.TOP or Gravity.CENTER_HORIZONTAL

        conteudo.addView(
            titulo(
                "PRÓXIMOS VENCIMENTOS",
                22f
            )
        )

        val hoje = Calendar.getInstance()

        val clientes =
            carregarClientes()
                .filter {

                    val vencimento =
                        proximoVencimento(it.dia)

                    val dias =
                        diasAte(vencimento)

                    dias in 0L..3L
                }
                .sortedBy {

                    proximoVencimento(
                        it.dia
                    ).timeInMillis
                }

        if (clientes.isEmpty()) {

            conteudo.addView(
                texto(
                    "Nenhum cliente vence nos próximos 3 dias.",
                    17f
                )
            )

        } else {

            clientes.forEach { cliente ->

                val vencimento =
                    proximoVencimento(
                        cliente.dia
                    )

                val dias =
                    diasAte(vencimento)

                val data =
                    formatarData(
                        vencimento
                    )

                val bloco =
                    TextView(this).apply {

                        text =
                            "${cliente.nome}\n" +
                                    "$data  •  " +
                                    when (dias) {
                                        0L -> "HOJE"
                                        1L -> "AMANHÃ"
                                        else -> "$dias dias"
                                    }

                        textSize = 15f

                        setTextColor(branco)

                        gravity =
                            Gravity.CENTER_VERTICAL

                        setPadding(
                            18,
                            8,
                            18,
                            8
                        )

                        background =
                            android.graphics.drawable.GradientDrawable().apply {

                                setColor(
                                    Color.rgb(
                                        15,
                                        15,
                                        15
                                    )
                                )

                                setStroke(
                                    2,
                                    azul
                                )

                                cornerRadius = 12f
                            }

                        setOnClickListener {

                            mostrarDetalhesCliente(
                                cliente
                            )
                        }
                    }

                conteudo.addView(
                    bloco,
                    LinearLayout.LayoutParams(
                        -1,
                        78
                    )
                )

                adicionarEspaco(
                    conteudo,
                    10
                )
            }
        }

        adicionarEspaco(
            conteudo,
            20
        )

        val voltar =
            botao(
                "VOLTAR",
                Color.rgb(
                    100,
                    100,
                    100
                )
            ) {

                mostrarInicio()
            }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        scroll.addView(conteudo)

        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(tela)
    }

    // ============================================================
    // VOLTAR DO ANDROID
    // ============================================================

    override fun onBackPressed() {

        mostrarInicio()
    }
}
