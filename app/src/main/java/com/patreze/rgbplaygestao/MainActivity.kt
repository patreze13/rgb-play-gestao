package com.patreze.rgbplaygestao

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Cliente(
    var nome: String,
    var contato: String,
    var dia: Int
)

class MainActivity : Activity() {

    private val fundo = Color.BLACK
    private val branco = Color.WHITE
    private val cinza = Color.rgb(165, 165, 165)

    private val vermelho = Color.rgb(235, 55, 70)
    private val verde = Color.rgb(45, 210, 100)
    private val azul = Color.rgb(55, 125, 255)

    private val cinzaBorda = Color.rgb(90, 90, 90)
    private val fundoCampo = Color.rgb(18, 18, 18)

    private val preferencias by lazy {
        getSharedPreferences(
            "rgb_play_gestao",
            Context.MODE_PRIVATE
        )
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

        val texto = preferencias.getString(
            "clientes",
            "[]"
        ) ?: "[]"

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

    private fun salvarClientes(
        clientes: List<Cliente>
    ) {

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
            .putString(
                "clientes",
                array.toString()
            )
            .apply()
    }

    // ============================================================
    // DATAS
    // ============================================================

    private fun proximoVencimento(
        dia: Int
    ): Calendar {

        val hoje = Calendar.getInstance()

        var ano = hoje.get(Calendar.YEAR)
        var mes = hoje.get(Calendar.MONTH)

        while (true) {

            val tentativa = Calendar.getInstance()

            tentativa.set(
                ano,
                mes,
                1,
                0,
                0,
                0
            )

            tentativa.set(
                Calendar.MILLISECOND,
                0
            )

            val ultimoDia =
                tentativa.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

            tentativa.set(
                ano,
                mes,
                minOf(dia, ultimoDia),
                0,
                0,
                0
            )

            if (!tentativa.before(hoje)) {
                return tentativa
            }

            mes++

            if (mes > Calendar.DECEMBER) {
                mes = Calendar.JANUARY
                ano++
            }
        }
    }

    private fun diasAte(
        calendario: Calendar
    ): Long {

        val hoje = Calendar.getInstance()

        hoje.set(
            hoje.get(Calendar.YEAR),
            hoje.get(Calendar.MONTH),
            hoje.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )

        hoje.set(
            Calendar.MILLISECOND,
            0
        )

        val data =
            calendario.clone() as Calendar

        data.set(
            data.get(Calendar.YEAR),
            data.get(Calendar.MONTH),
            data.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )

        data.set(
            Calendar.MILLISECOND,
            0
        )

        return (
            data.timeInMillis -
                hoje.timeInMillis
            ) / (
            1000L * 60L * 60L * 24L
        )
    }

    private fun formatarData(
        calendario: Calendar
    ): String {

        return SimpleDateFormat(
            "dd/MM/yyyy",
            Locale("pt", "BR")
        ).format(calendario.time)
    }

    private fun formatarDiaMes(
        calendario: Calendar
    ): String {

        return SimpleDateFormat(
            "dd/MM",
            Locale("pt", "BR")
        ).format(calendario.time)
    }

    // ============================================================
    // BASE VISUAL
    // ============================================================

    private fun criarBase(): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_HORIZONTAL

            setBackgroundColor(fundo)

            setPadding(
                24,
                20,
                24,
                20
            )
        }
    }

    /*
     * Esta é a mudança principal do layout.
     *
     * O ScrollView ocupa toda a tela.
     * O conteúdo interno usa fillViewport + CENTER_VERTICAL.
     *
     * Se o conteúdo couber:
     *     fica centralizado verticalmente.
     *
     * Se não couber:
     *     começa normalmente no topo e pode rolar.
     */

    private fun criarAreaCentral(
        conteudo: LinearLayout
    ): ScrollView {

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true

                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS

                setBackgroundColor(fundo)
            }

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL or
                Gravity.CENTER_VERTICAL

        scroll.addView(
            conteudo,
            ScrollView.LayoutParams(
                -1,
                -1
            )
        )

        return scroll
    }

    private fun adicionarNaTela(
        tela: LinearLayout,
        scroll: ScrollView
    ) {

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
    // TEXTOS
    // ============================================================

    private fun titulo(
        texto: String,
        tamanho: Float = 24f
    ): TextView {

        return TextView(this).apply {

            text = texto

            textSize = tamanho

            setTextColor(branco)

            gravity =
                Gravity.CENTER

            typeface =
                Typeface.DEFAULT_BOLD

            maxLines = 2

            setPadding(
                8,
                0,
                8,
                12
            )
        }
    }

    private fun texto(
        texto: String,
        tamanho: Float = 15f
    ): TextView {

        return TextView(this).apply {

            text = texto

            textSize = tamanho

            setTextColor(branco)

            gravity =
                Gravity.CENTER

            setPadding(
                8,
                8,
                8,
                8
            )
        }
    }

    // ============================================================
    // BOTÕES
    // ============================================================

    private fun botao(
        texto: String,
        corBorda: Int,
        altura: Int = 68,
        tamanhoFonte: Float = 16f,
        acao: () -> Unit
    ): TextView {

        return TextView(this).apply {

            this.text = texto

            textSize = tamanhoFonte

            setTextColor(branco)

            gravity =
                Gravity.CENTER

            typeface =
                Typeface.DEFAULT_BOLD

            maxLines = 2

            setPadding(
                18,
                8,
                18,
                8
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.rgb(
                            8,
                            8,
                            8
                        )
                    )

                    setStroke(
                        3,
                        corBorda
                    )

                    cornerRadius = 20f
                }

            isClickable = true

            setOnClickListener {
                acao()
            }

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    altura
                ).apply {
                    topMargin = 7
                    bottomMargin = 7
                }
        }
    }

    private fun espaco(
        altura: Int
    ): Space {

        return Space(this).apply {

            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    altura
                )
        }
    }

    // ============================================================
    // LOGO
    // ============================================================

    private fun criarLogo(): LinearLayout {

        val caixa =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    10,
                    0,
                    10,
                    22
                )
            }

        val logo =
            TextView(this).apply {

                text = "RGB"

                textSize = 42f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(branco)

                setPadding(
                    0,
                    0,
                    0,
                    0
                )
            }

        /*
         * RGB separado em três TextViews para manter
         * a identidade visual sem depender de drawable externo.
         */

        val rgb =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER
            }

        val r =
            TextView(this).apply {
                text = "R"
                textSize = 42f
                setTextColor(vermelho)
                typeface =
                    Typeface.DEFAULT_BOLD
            }

        val g =
            TextView(this).apply {
                text = "G"
                textSize = 42f
                setTextColor(verde)
                typeface =
                    Typeface.DEFAULT_BOLD
            }

        val b =
            TextView(this).apply {
                text = "B"
                textSize = 42f
                setTextColor(azul)
                typeface =
                    Typeface.DEFAULT_BOLD
            }

        rgb.addView(r)
        rgb.addView(g)
        rgb.addView(b)

        /*
         * Mantém "PLAY" branco abaixo do RGB.
         */

        val play =
            TextView(this).apply {

                text = "— PLAY —"

                textSize = 15f

                setTextColor(branco)

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.DEFAULT_BOLD
            }

        caixa.addView(rgb)
        caixa.addView(play)

        val subtitulo =
            TextView(this).apply {

                text = "GESTÃO DE CLIENTES"

                textSize = 12f

                setTextColor(cinza)

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        caixa.addView(subtitulo)

        return caixa
    }

    // ============================================================
    // INÍCIO
    // ============================================================

    private fun mostrarInicio() {

        val tela =
            criarBase()

        val conteudo =
            criarBase()

        conteudo.gravity =
            Gravity.CENTER

        conteudo.setPadding(
            20,
            30,
            20,
            30
        )

        conteudo.addView(
            criarLogo(),
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        conteudo.addView(
            botao(
                "ADICIONAR CLIENTE",
                vermelho,
                82,
                17f
            ) {
                mostrarAdicionarCliente()
            }
        )

        conteudo.addView(
            botao(
                "VER CLIENTES",
                verde,
                82,
                17f
            ) {
                mostrarClientes()
            }
        )

        conteudo.addView(
            botao(
                "PRÓXIMOS VENCIMENTOS",
                azul,
                82,
                17f
            ) {
                mostrarProximosVencimentos()
            }
        )

        val scroll =
            criarAreaCentral(
                conteudo
            )

        adicionarNaTela(
            tela,
            scroll
        )
    }

    // ============================================================
    // CAMPOS
    // ============================================================

    private fun campo(
        dica: String,
        tipo: Int =
            InputType.TYPE_CLASS_TEXT
    ): EditText {

        return EditText(this).apply {

            hint = dica

            setHintTextColor(
                ColorStateList.valueOf(
                    Color.rgb(
                        130,
                        130,
                        130
                    )
                )
            )

            setTextColor(branco)

            textSize = 15f

            inputType = tipo

            setSingleLine(true)

            setPadding(
                18,
                0,
                18,
                0
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        fundoCampo
                    )

                    setStroke(
                        1,
                        cinzaBorda
                    )

                    cornerRadius = 15f
                }
        }
    }

    // ============================================================
    // MÁSCARA TELEFONE
    // ============================================================

    private fun aplicarMascaraTelefone(
        campo: EditText
    ) {

        var alterando = false

        campo.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {

                    if (alterando) {
                        return
                    }

                    val numeros =
                        s?.toString()
                            ?.filter {
                                it.isDigit()
                            }
                            ?: ""

                    val limitado =
                        numeros.take(11)

                    val formatado =
                        formatarTelefone(
                            limitado
                        )

                    if (
                        s?.toString() !=
                        formatado
                    ) {

                        alterando = true

                        campo.setText(
                            formatado
                        )

                        campo.setSelection(
                            formatado.length
                        )

                        alterando = false
                    }
                }
            }
        )
    }

    private fun formatarTelefone(
        numeros: String
    ): String {

        if (numeros.isEmpty()) {
            return ""
        }

        if (numeros.length <= 2) {
            return "($numeros"
        }

        if (numeros.length <= 7) {

            return "(" +
                numeros.substring(
                    0,
                    2
                ) +
                ") " +
                numeros.substring(2)
        }

        if (numeros.length <= 11) {

            val ddd =
                numeros.substring(
                    0,
                    2
                )

            val restante =
                numeros.substring(2)

            return if (
                restante.length <= 5
            ) {

                "($ddd) $restante"

            } else {

                "(" +
                    ddd +
                    ") " +
                    restante.substring(
                        0,
                        5
                    ) +
                    "-" +
                    restante.substring(5)
            }
        }

        return numeros
    }

    // ============================================================
    // ADICIONAR / EDITAR
    // ============================================================

    private fun mostrarAdicionarCliente(
        clienteExistente: Cliente? = null
    ) {

        val editando =
            clienteExistente != null

        val tela =
            criarBase()

        val conteudo =
            criarBase()

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL or
                Gravity.CENTER_VERTICAL

        conteudo.setPadding(
            0,
            20,
            0,
            20
        )

        conteudo.addView(
            titulo(
                if (editando)
                    "EDITAR CLIENTE"
                else
                    "ADICIONAR CLIENTE",
                24f
            )
        )

        val nome =
            campo(
                "Nome do cliente"
            )

        val contato =
            campo(
                "WhatsApp / telefone",
                InputType.TYPE_CLASS_PHONE
            )

        aplicarMascaraTelefone(
            contato
        )

        val dia =
            campo(
                "Dia da contratação (1 a 31)",
                InputType.TYPE_CLASS_NUMBER
            )

        dia.filters =
            arrayOf(
                InputFilter.LengthFilter(2)
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

        val larguraCampo =
            LinearLayout.LayoutParams(
                -1,
                62
            ).apply {
                topMargin = 6
                bottomMargin = 6
            }

        conteudo.addView(
            nome,
            larguraCampo
        )

        conteudo.addView(
            contato,
            LinearLayout.LayoutParams(
                -1,
                62
            ).apply {
                topMargin = 6
                bottomMargin = 6
            }
        )

        conteudo.addView(
            dia,
            LinearLayout.LayoutParams(
                -1,
                62
            ).apply {
                topMargin = 6
                bottomMargin = 6
            }
        )

        conteudo.addView(
            botao(
                if (editando)
                    "SALVAR ALTERAÇÕES"
                else
                    "CADASTRAR CLIENTE",
                if (editando)
                    azul
                else
                    verde,
                72,
                16f
            ) {

                val nomeTexto =
                    nome.text
                        .toString()
                        .trim()

                val contatoTexto =
                    contato.text
                        .toString()
                        .trim()

                val diaTexto =
                    dia.text
                        .toString()
                        .trim()

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
                            it.nome ==
                                antigo.nome
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

                salvarClientes(
                    clientes
                )

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
        )

        conteudo.addView(
            botao(
                "VOLTAR",
                cinzaBorda,
                62,
                15f
            ) {
                mostrarInicio()
            }
        )

        adicionarNaTela(
            tela,
            criarAreaCentral(
                conteudo
            )
        )
    }

    // ============================================================
    // CLIENTES
    // ============================================================

    private fun mostrarClientes() {

        val tela =
            criarBase()

        val conteudo =
            criarBase()

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL or
                Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo(
                "CLIENTES",
                24f
            )
        )

        val clientes =
            carregarClientes()

        clientes.sortBy {

            proximoVencimento(
                it.dia
            ).timeInMillis
        }

        if (clientes.isEmpty()) {

            conteudo.addView(
                texto(
                    "Nenhum cliente cadastrado.",
                    15f
                )
            )

        } else {

            clientes.forEach { cliente ->

                conteudo.addView(
                    criarBlocoCliente(
                        cliente
                    )
                )

                conteudo.addView(
                    espaco(10)
                )
            }
        }

        conteudo.addView(
            botao(
                "VOLTAR",
                cinzaBorda,
                62,
                15f
            ) {
                mostrarInicio()
            }
        )

        adicionarNaTela(
            tela,
            criarAreaCentral(
                conteudo
            )
        )
    }

    private fun criarBlocoCliente(
        cliente: Cliente
    ): TextView {

        val vencimento =
            proximoVencimento(
                cliente.dia
            )

        val data =
            formatarDiaMes(
                vencimento
            )

        return TextView(this).apply {

            text =
                "${cliente.nome}   •   Vence todo dia ${cliente.dia}"

            textSize = 14f

            setTextColor(branco)

            gravity =
                Gravity.CENTER_VERTICAL

            maxLines = 1

            ellipsize =
                android.text.TextUtils.TruncateAt.END

            setPadding(
                18,
                0,
                18,
                0
            )

            background =
                GradientDrawable().apply {

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

                    cornerRadius = 14f
                }

            setOnClickListener {

                mostrarDetalhesCliente(
                    cliente
                )
            }

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    66
                )
        }
    }

    // ============================================================
    // DETALHES
    // ============================================================

    private fun mostrarDetalhesCliente(
        cliente: Cliente
    ) {

        val tela =
            criarBase()

        val conteudo =
            criarBase()

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL or
                Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo(
                cliente.nome,
                23f
            )
        )

        val vencimento =
            proximoVencimento(
                cliente.dia
            )

        val dias =
            diasAte(
                vencimento
            )

        val contatoExibicao =
            if (
                cliente.contato.isEmpty()
            )
                "Não informado"
            else
                cliente.contato

        val contato =
            TextView(this).apply {

                text =
                    "WhatsApp\n$contatoExibicao"

                textSize = 15f

                setTextColor(
                    if (
                        cliente.contato.isEmpty()
                    )
                        cinza
                    else
                        verde
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    12,
                    12,
                    12,
                    12
                )

                if (
                    cliente.contato.isNotEmpty()
                ) {

                    isClickable = true

                    setOnClickListener {

                        abrirWhatsApp(
                            cliente.contato
                        )
                    }
                }
            }

        conteudo.addView(
            contato,
            LinearLayout.LayoutParams(
                -1,
                72
            )
        )

        conteudo.addView(
            texto(
                "Dia da contratação\n" +
                    "Todo dia ${cliente.dia}",
                15f
            )
        )

        conteudo.addView(
            texto(
                "Próximo vencimento\n" +
                    formatarData(
                        vencimento
                    ) +
                    "\n" +
                    when (dias) {

                        0L ->
                            "VENCE HOJE"

                        1L ->
                            "VENCE AMANHÃ"

                        else ->
                            "Faltam $dias dias"
                    },
                15f
            )
        )

        conteudo.addView(
            espaco(12)
        )

        if (
            cliente.contato.isNotEmpty()
        ) {

            conteudo.addView(
                botao(
                    "ABRIR WHATSAPP",
                    verde,
                    68,
                    16f
                ) {

                    abrirWhatsApp(
                        cliente.contato
                    )
                }
            )

            conteudo.addView(
                espaco(4)
            )
        }

        conteudo.addView(
            botao(
                "EDITAR CLIENTE",
                azul,
                68,
                16f
            ) {

                mostrarAdicionarCliente(
                    cliente
                )
            }
        )

        conteudo.addView(
            botao(
                "EXCLUIR CLIENTE",
                vermelho,
                68,
                16f
            ) {

                confirmarExclusao(
                    cliente
                )
            }
        )

        conteudo.addView(
            botao(
                "VOLTAR",
                cinzaBorda,
                62,
                15f
            ) {

                mostrarClientes()
            }
        )

        adicionarNaTela(
            tela,
            criarAreaCentral(
                conteudo
            )
        )
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

        if (
            numero.length == 10 ||
            numero.length == 11
        ) {

            numero =
                "55$numero"
        }

        if (
            numero.length < 12
        ) {

            Toast.makeText(
                this,
                "Número de WhatsApp inválido.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val url =
                "https://wa.me/$numero"

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            if (
                intent.resolveActivity(
                    packageManager
                ) != null
            ) {

                startActivity(
                    intent
                )

            } else {

                Toast.makeText(
                    this,
                    "Não foi possível abrir o WhatsApp.",
                    Toast.LENGTH_SHORT
                ).show()
            }

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

            .setTitle(
                "Excluir cliente"
            )

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

                    it.nome ==
                        cliente.nome
                }

                salvarClientes(
                    clientes
                )

                mostrarClientes()
            }

            .show()
    }

    // ============================================================
    // PRÓXIMOS VENCIMENTOS
    // ============================================================

    private fun mostrarProximosVencimentos() {

        val tela =
            criarBase()

        val conteudo =
            criarBase()

        conteudo.gravity =
            Gravity.CENTER_HORIZONTAL or
                Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo(
                "PRÓXIMOS VENCIMENTOS",
                22f
            )
        )

        val clientes =
            carregarClientes()
                .filter {

                    val vencimento =
                        proximoVencimento(
                            it.dia
                        )

                    val dias =
                        diasAte(
                            vencimento
                        )

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
                    "Nenhum cliente vence\n" +
                        "nos próximos 3 dias.",
                    16f
                )
            )

        } else {

            clientes.forEach { cliente ->

                val vencimento =
                    proximoVencimento(
                        cliente.dia
                    )

                val dias =
                    diasAte(
                        vencimento
                    )

                val status =
                    when (dias) {

                        0L ->
                            "HOJE"

                        1L ->
                            "AMANHÃ"

                        else ->
                            "EM $dias DIAS"
                    }

                val bloco =
                    TextView(this).apply {

                        text =
                            "${cliente.nome}\n" +
                                "${formatarData(vencimento)}  •  $status"

                        textSize = 14f

                        setTextColor(
                            branco
                        )

                        gravity =
                            Gravity.CENTER

                        setPadding(
                            12,
                            8,
                            12,
                            8
                        )

                        background =
                            GradientDrawable().apply {

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

                                cornerRadius =
                                    14f
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
                    ).apply {
                        topMargin = 5
                        bottomMargin = 5
                    }
                )
            }
        }

        conteudo.addView(
            espaco(12)
        )

        conteudo.addView(
            botao(
                "VOLTAR",
                cinzaBorda,
                62,
                15f
            ) {

                mostrarInicio()
            }
        )

        adicionarNaTela(
            tela,
            criarAreaCentral(
                conteudo
            )
        )
    }

    // ============================================================
    // VOLTAR
    // ============================================================

    @Deprecated("Compatibilidade com Android")
    override fun onBackPressed() {

        mostrarInicio()
    }
}
