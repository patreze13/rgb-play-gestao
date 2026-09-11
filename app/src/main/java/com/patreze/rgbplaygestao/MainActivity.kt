package com.patreze.rgbplaygestao

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Stack
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class Cliente(
    var nome: String,
    var contato: String,
    var dia: Int,
    var ultimoMesPago: String = ""
)

class MainActivity : Activity() {

    private val preferencias by lazy {
        getSharedPreferences("rgb_play_gestao", Context.MODE_PRIVATE)
    }

    private val CODIGO_SELECIONAR_BACKUP = 1001
    private val CODIGO_PERMISSAO_NOTIFICACAO = 1002

    private val pilhaTelas = Stack<() -> Unit>()
    private var navegandoVoltar = false

    private val fundoCard = Color.rgb(20, 20, 22)
    private val fundoCampo = Color.rgb(18, 18, 20)
    private val branco = Color.rgb(255, 255, 255)
    private val cinza = Color.rgb(160, 160, 160)
    private val cinzaBorda = Color.rgb(55, 55, 60)
    private val verde = Color.rgb(0, 230, 118)
    private val vermelho = Color.rgb(255, 61, 0)
    private val azul = Color.rgb(41, 121, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        solicitarPermissaoNotificacao()
        agendarNotificacoesVencimento()

        mostrarInicio()
    }

    private fun navegarPara(acao: () -> Unit) {
        if (!navegandoVoltar) {
            pilhaTelas.push(acao)
        }
        navegandoVoltar = false
        acao()
    }

    override fun onBackPressed() {
        if (pilhaTelas.size > 1) {
            pilhaTelas.pop()
            val telaAnterior = pilhaTelas.peek()
            navegandoVoltar = true
            telaAnterior.invoke()
        } else {
            finish()
        }
    }

    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    CODIGO_PERMISSAO_NOTIFICACAO
                )
            }
        }
    }

    private fun agendarNotificacoesVencimento() {
        try {
            val requisicaoTrabalho = PeriodicWorkRequestBuilder<VencimentoWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "VerificacaoVencimentos",
                ExistingPeriodicWorkPolicy.KEEP,
                requisicaoTrabalho
            )
        } catch (_: Exception) {
        }
    }

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
                        dia = objeto.optInt("dia"),
                        ultimoMesPago = objeto.optString("ultimoMesPago", "")
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
            val objeto = JSONObject().apply {
                put("nome", cliente.nome)
                put("contato", cliente.contato)
                put("dia", cliente.dia)
                put("ultimoMesPago", cliente.ultimoMesPago)
            }
            array.put(objeto)
        }
        preferencias.edit().putString("clientes", array.toString()).apply()
    }

    private fun chaveMes(cal: Calendar): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    private fun proximoVencimento(cliente: Cliente): Calendar {
        val hoje = Calendar.getInstance().inicioDoDia()
        var ano = hoje.get(Calendar.YEAR)
        var mes = hoje.get(Calendar.MONTH)

        while (true) {
            val tentativa = Calendar.getInstance().apply {
                set(ano, mes, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val ultimoDia = tentativa.getActualMaximum(Calendar.DAY_OF_MONTH)
            tentativa.set(ano, mes, minOf(cliente.dia, ultimoDia), 0, 0, 0)

            val chaveTentativa = chaveMes(tentativa)
            val naoPassouDeHoje = !tentativa.before(hoje)
            val mesNaoPago = cliente.ultimoMesPago != chaveTentativa

            if (naoPassouDeHoje && mesNaoPago) {
                return tentativa
            }

            mes++
            if (mes > Calendar.DECEMBER) {
                mes = Calendar.JANUARY
                ano++
            }
        }
    }

    private fun diasAte(calendario: Calendar): Long {
        val hoje = Calendar.getInstance().inicioDoDia()
        val data = (calendario.clone() as Calendar).inicioDoDia()
        return (data.timeInMillis - hoje.timeInMillis) / (1000L * 60L * 60L * 24L)
    }

    private fun Calendar.inicioDoDia(): Calendar {
        return (clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatarData(calendario: Calendar): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(calendario.time)
    }

    private fun formatarDiaMes(calendario: Calendar): String {
        return SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(calendario.time)
    }

    private fun obterFundoComTextura(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.rgb(20, 20, 24),
                Color.rgb(10, 10, 12),
                Color.rgb(5, 5, 6)
            )
        )
    }

    private fun criarBase(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = obterFundoComTextura()
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
    }

    private fun criarAreaCentral(conteudo: LinearLayout): ScrollView {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            background = obterFundoComTextura()
        }
        scroll.addView(
            conteudo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return scroll
    }

    private fun adicionarNaTela(tela: LinearLayout, scroll: ScrollView) {
        tela.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        setContentView(tela)
    }

    private fun titulo(texto: String, tamanho: Float = 22f): TextView {
        return TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(branco)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, dp(6), 8, dp(14))
        }
    }

    private fun texto(texto: String, tamanho: Float = 15f): TextView {
        return TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(branco)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
    }

    private fun botao(
        texto: String,
        corBorda: Int,
        altura: Int = 62,
        tamanhoFonte: Float = 15f,
        acao: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = texto
            textSize = tamanhoFonte
            setTextColor(branco)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            setPadding(18, 8, 18, 8)
            background = GradientDrawable().apply {
                setColor(Color.rgb(16, 16, 18))
                setStroke(dp(2), corBorda)
                cornerRadius = dp(16).toFloat()
            }
            isClickable = true
            setOnClickListener { acao() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(altura)
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        }
    }

    private fun espaco(altura: Int): Space {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(altura))
        }
    }

    private fun dp(valor: Int): Int {
        val densidade = resources.displayMetrics.density
        return (valor * densidade).toInt()
    }

    private fun criarLogo(): LinearLayout {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(10, dp(6), 10, dp(16))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_rgb)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        caixa.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(210),
                dp(160)
            )
        )

        val subtitulo = TextView(this).apply {
            text = "GESTÃO DE CLIENTES"
            textSize = 12f
            setTextColor(cinza)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }

        caixa.addView(subtitulo)
        return caixa
    }

    private fun mostrarInicio() {
        pilhaTelas.clear()
        pilhaTelas.push { mostrarInicio() }

        val tela = criarBase()
        val conteudo = criarBase().apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        conteudo.addView(criarLogo())
        conteudo.addView(espaco(6))

        conteudo.addView(
            botao("ADICIONAR CLIENTE", vermelho, 64, 15f) {
                navegarPara { mostrarAdicionarCliente() }
            }
        )

        conteudo.addView(
            botao("VER CLIENTES", verde, 64, 15f) {
                navegarPara { mostrarClientes() }
            }
        )

        conteudo.addView(
            botao("PRÓXIMOS VENCIMENTOS", azul, 64, 15f) {
                navegarPara { mostrarProximosVencimentos() }
            }
        )

        conteudo.addView(espaco(12))

        val containerBackup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnExportar = TextView(this).apply {
            text = "EXPORTAR BACKUP"
            textSize = 12f
            setTextColor(branco)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, dp(12), 8, dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(18, 18, 20))
                setStroke(2, cinzaBorda)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { exportarBackup() }
        }
        val paramsExportar = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = dp(6)
        }
        containerBackup.addView(btnExportar, paramsExportar)

        val btnImportar = TextView(this).apply {
            text = "RESTAURAR BACKUP"
            textSize = 12f
            setTextColor(branco)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, dp(12), 8, dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(18, 18, 20))
                setStroke(2, cinzaBorda)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { abrirSeletorArquivoBackup() }
        }
        val paramsImportar = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(6)
        }
        containerBackup.addView(btnImportar, paramsImportar)

        conteudo.addView(containerBackup)

        val scroll = criarAreaCentral(conteudo)
        adicionarNaTela(tela, scroll)
    }

    private fun exportarBackup() {
        val textoClientes = preferencias.getString("clientes", "[]") ?: "[]"
        if (textoClientes == "[]") {
            Toast.makeText(this, "Nenhum cliente cadastrado para backup.", Toast.LENGTH_SHORT).show()
            return
        }

        Executors.newSingleThreadExecutor().execute {
            try {
                val nomeArquivo = "backup_rgb_play_" +
                    SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) + ".json"
                val bytes = textoClientes.toByteArray(Charsets.UTF_8)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, nomeArquivo)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download")
                    }

                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw Exception("Erro ao criar arquivo.")

                    contentResolver.openOutputStream(uri).use { saida ->
                        saida?.write(bytes)
                    }

                    runOnUiThread {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, "Salvar backup / Enviar"))
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Requer Android 10 ou superior.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erro no backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirSeletorArquivoBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, CODIGO_SELECIONAR_BACKUP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_SELECIONAR_BACKUP && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                restaurarBackup(uri)
            }
        }
    }

    private fun restaurarBackup(uri: Uri) {
        try {
            val conteudo = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: ""
            val jsonArray = JSONArray(conteudo)
            if (jsonArray.length() > 0) {
                preferencias.edit().putString("clientes", conteudo).apply()
                Toast.makeText(this, "${jsonArray.length()} clientes restaurados com sucesso!", Toast.LENGTH_LONG).show()
                mostrarInicio()
            } else {
                Toast.makeText(this, "Arquivo vazio ou inválido.", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Falha ao ler arquivo de backup JSON.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun campo(dica: String, tipo: Int = InputType.TYPE_CLASS_TEXT): EditText {
        return EditText(this).apply {
            hint = dica
            setHintTextColor(ColorStateList.valueOf(Color.rgb(120, 120, 125)))
            setTextColor(branco)
            textSize = 15f
            inputType = tipo
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply {
                setColor(fundoCampo)
                setStroke(1, cinzaBorda)
                cornerRadius = dp(14).toFloat()
            }
        }
    }

    private fun aplicarMascaraTelefone(campo: EditText) {
        var alterando = false
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (alterando) return
                val numeros = s?.toString()?.filter { it.isDigit() } ?: ""
                val limitado = numeros.take(11)
                val formatado = formatarTelefone(limitado)
                if (s?.toString() != formatado) {
                    alterando = true
                    campo.setText(formatado)
                    campo.setSelection(formatado.length)
                    alterando = false
                }
            }
        })
    }

    private fun formatarTelefone(numeros: String): String {
        if (numeros.isEmpty()) return ""
        if (numeros.length <= 2) return "($numeros"
        if (numeros.length <= 7) return "(" + numeros.substring(0, 2) + ") " + numeros.substring(2)
        if (numeros.length <= 11) {
            val ddd = numeros.substring(0, 2)
            val restante = numeros.substring(2)
            return if (restante.length <= 5) {
                "($ddd) $restante"
            } else {
                "(" + ddd + ") " + restante.substring(0, 5) + "-" + restante.substring(5)
            }
        }
        return numeros
    }

    private fun mostrarAdicionarCliente(clienteExistente: Cliente? = null) {
        val editando = clienteExistente != null
        val tela = criarBase()
        val conteudo = criarBase().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }

        conteudo.addView(titulo(if (editando) "EDITAR CLIENTE" else "ADICIONAR CLIENTE", 22f))

        val nome = campo("Nome do cliente")
        val contato = campo("WhatsApp / telefone", InputType.TYPE_CLASS_PHONE)
        aplicarMascaraTelefone(contato)

        val dia = campo("Dia da contratação (1 a 31)", InputType.TYPE_CLASS_NUMBER).apply {
            filters = arrayOf(InputFilter.LengthFilter(2))
        }

        if (clienteExistente != null) {
            nome.setText(clienteExistente.nome)
            contato.setText(clienteExistente.contato)
            dia.setText(clienteExistente.dia.toString())
        }

        val paramsCampo = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(54)
        ).apply {
            topMargin = dp(6)
            bottomMargin = dp(6)
        }

        conteudo.addView(nome, paramsCampo)
        conteudo.addView(contato, paramsCampo)
        conteudo.addView(dia, paramsCampo)

        conteudo.addView(espaco(10))

        conteudo.addView(
            botao(
                if (editando) "SALVAR ALTERAÇÕES" else "CADASTRAR CLIENTE",
                if (editando) azul else verde,
                62,
                15f
            ) {
                val nomeTexto = nome.text.toString().trim()
                val contatoTexto = contato.text.toString().trim()
                val diaTexto = dia.text.toString().trim()

                if (nomeTexto.isEmpty()) {
                    Toast.makeText(this, "Informe o nome do cliente.", Toast.LENGTH_SHORT).show()
                    return@botao
                }

                val diaNumero = diaTexto.toIntOrNull()
                if (diaNumero == null || diaNumero !in 1..31) {
                    Toast.makeText(this, "Informe um dia entre 1 e 31.", Toast.LENGTH_SHORT).show()
                    return@botao
                }

                val clientes = carregarClientes()

                if (!editando) {
                    if (clientes.any { it.nome.equals(nomeTexto, ignoreCase = true) }) {
                        Toast.makeText(this, "Esse cliente já está cadastrado.", Toast.LENGTH_SHORT).show()
                        return@botao
                    }

                    val mesAtualQuitado = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                    clientes.add(Cliente(nomeTexto, contatoTexto, diaNumero, ultimoMesPago = mesAtualQuitado))
                } else {
                    val cliente = clientes.find { it.nome == clienteExistente!!.nome }
                    if (cliente != null) {
                        cliente.nome = nomeTexto
                        cliente.contato = contatoTexto
                        cliente.dia = diaNumero
                    }
                }

                salvarClientes(clientes)
                Toast.makeText(this, if (editando) "Cliente atualizado." else "Cliente cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        )

        conteudo.addView(
            botao("VOLTAR", cinzaBorda, 56, 14f) {
                onBackPressed()
            }
        )

        adicionarNaTela(tela, criarAreaCentral(conteudo))
    }

    private fun mostrarClientes() {
        val tela = criarBase()
        val conteudo = criarBase().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        conteudo.addView(titulo("CLIENTES", 22f))

        val todosClientes = carregarClientes()
        todosClientes.sortBy { proximoVencimento(it).timeInMillis }

        val campoBusca = campo("Pesquisar por nome ou contato...").apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(14)
            }
        }
        conteudo.addView(campoBusca)

        val containerLista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        conteudo.addView(containerLista)

        fun renderizarLista(filtro: String) {
            containerLista.removeAllViews()

            val filtrados = if (filtro.isEmpty()) {
                todosClientes
            } else {
                todosClientes.filter {
                    it.nome.contains(filtro, ignoreCase = true) || it.contato.contains(filtro)
                }
            }

            if (filtrados.isEmpty()) {
                containerLista.addView(texto(if (filtro.isEmpty()) "Nenhum cliente cadastrado." else "Nenhum cliente encontrado.", 15f))
                return
            }

            filtrados.forEach { cliente ->
                val dias = diasAte(proximoVencimento(cliente))
                val estaCritico = dias in 0L..3L
                val card = criarCardRetangular(cliente, estaCritico) {
                    navegarPara { mostrarDetalhesCliente(cliente) }
                }

                containerLista.addView(
                    card,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                )
            }
        }

        renderizarLista("")

        campoBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderizarLista(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        conteudo.addView(espaco(10))
        conteudo.addView(
            botao("VOLTAR", cinzaBorda, 56, 14f) {
                onBackPressed()
            }
        )

        adicionarNaTela(tela, criarAreaCentral(conteudo))
    }

    private fun criarCardRetangular(
        cliente: Cliente,
        critico: Boolean,
        acao: () -> Unit
    ): LinearLayout {
        val corDestaque = if (critico) vermelho else verde
        val vencimento = proximoVencimento(cliente)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(fundoCard)
                setStroke(dp(1), corDestaque)
                cornerRadius = dp(12).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { acao() }

            val colunaInfo = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val txtNome = TextView(context).apply {
                text = cliente.nome
                textSize = 15f
                setTextColor(branco)
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            colunaInfo.addView(txtNome)

            val txtSub = TextView(context).apply {
                text = "Todo dia ${cliente.dia}"
                textSize = 12f
                setTextColor(cinza)
                setPadding(0, dp(2), 0, 0)
            }
            colunaInfo.addView(txtSub)

            addView(colunaInfo)

            val colunaData = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            val txtVenc = TextView(context).apply {
                text = formatarDiaMes(vencimento)
                textSize = 16f
                setTextColor(corDestaque)
                typeface = Typeface.DEFAULT_BOLD
            }
            colunaData.addView(txtVenc)

            val dias = diasAte(vencimento)
            val labelStatus = when (dias) {
                0L -> "Hoje"
                1L -> "Amanhã"
                in 2L..3L -> "Em $dias d"
                else -> "Em dia"
            }

            val txtLabel = TextView(context).apply {
                text = labelStatus
                textSize = 11f
                setTextColor(if (critico) vermelho else cinza)
            }
            colunaData.addView(txtLabel)

            addView(colunaData)
        }
    }

    private fun mostrarProximosVencimentos() {
        val tela = criarBase()
        val conteudo = criarBase().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        conteudo.addView(titulo("PRÓXIMOS VENCIMENTOS", 22f))

        val clientes = carregarClientes().filter {
            val vencimento = proximoVencimento(it)
            val dias = diasAte(vencimento)
            dias in 0L..3L
        }.sortedBy {
            proximoVencimento(it).timeInMillis
        }

        if (clientes.isEmpty()) {
            conteudo.addView(
                texto("Nenhum cliente vence\nnos próximos 3 dias.", 15f)
            )
        } else {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            clientes.forEach { cliente ->
                val card = criarCardRetangular(cliente, true) {
                    navegarPara { mostrarDetalhesCliente(cliente) }
                }

                container.addView(
                    card,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                )
            }

            conteudo.addView(container)
        }

        conteudo.addView(espaco(12))
        conteudo.addView(
            botao("VOLTAR", cinzaBorda, 56, 14f) {
                onBackPressed()
            }
        )

        adicionarNaTela(tela, criarAreaCentral(conteudo))
    }

    private fun mostrarDetalhesCliente(cliente: Cliente) {
        val tela = criarBase()
        val conteudo = criarBase().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(12), dp(4), dp(12))
        }

        conteudo.addView(titulo(cliente.nome, 22f))

        val vencimento = proximoVencimento(cliente)
        val dias = diasAte(vencimento)
        val contatoExibicao = if (cliente.contato.isEmpty()) "Não informado" else cliente.contato

        val contato = TextView(this).apply {
            text = "WhatsApp\n$contatoExibicao"
            textSize = 15f
            setTextColor(if (cliente.contato.isEmpty()) cinza else verde)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            if (cliente.contato.isNotEmpty()) {
                isClickable = true
                setOnClickListener { abrirWhatsApp(cliente.contato) }
            }
        }
        conteudo.addView(contato)

        conteudo.addView(
            texto("Dia da contratação\nTodo dia ${cliente.dia}", 14f)
        )

        val textoStatus = when (dias) {
            0L -> "VENCE HOJE"
            1L -> "VENCE AMANHÃ"
            else -> "Faltam $dias dias"
        }

        conteudo.addView(
            texto("Próximo vencimento\n${formatarData(vencimento)}\n$textoStatus", 15f)
        )

        conteudo.addView(espaco(10))

        conteudo.addView(
            botao("CONFIRMAR PAGAMENTO / RENOVAR", verde, 60, 14f) {
                confirmarBaixaPagamento(cliente)
            }
        )

        if (cliente.contato.isNotEmpty()) {
            conteudo.addView(
                botao("ABRIR WHATSAPP", verde, 60, 15f) {
                    abrirWhatsApp(cliente.contato)
                }
            )
        }

        conteudo.addView(
            botao("EDITAR CLIENTE", azul, 60, 15f) {
                navegarPara { mostrarAdicionarCliente(cliente) }
            }
        )

        conteudo.addView(
            botao("EXCLUIR CLIENTE", vermelho, 60, 15f) {
                confirmarExclusao(cliente)
            }
        )

        conteudo.addView(
            botao("VOLTAR", cinzaBorda, 56, 14f) {
                onBackPressed()
            }
        )

        adicionarNaTela(tela, criarAreaCentral(conteudo))
    }

    private fun confirmarBaixaPagamento(cliente: Cliente) {
        val vencimentoAtual = proximoVencimento(cliente)
        val dataFormatada = formatarData(vencimentoAtual)

        AlertDialog.Builder(this)
            .setTitle("Confirmar Pagamento")
            .setMessage("Confirmar pagamento referente ao vencimento de $dataFormatada?\nO próximo vencimento irá para o mês seguinte.")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("CONFIRMAR") { _, _ ->
                val clientes = carregarClientes()
                val cli = clientes.find { it.nome == cliente.nome }
                if (cli != null) {
                    cli.ultimoMesPago = chaveMes(vencimentoAtual)
                    salvarClientes(clientes)
                    cliente.ultimoMesPago = cli.ultimoMesPago
                    Toast.makeText(this, "Pagamento confirmado! Renovado até o próximo mês.", Toast.LENGTH_SHORT).show()
                    mostrarDetalhesCliente(cliente)
                }
            }
            .show()
    }

    private fun abrirWhatsApp(contato: String) {
        var numero = contato.filter { it.isDigit() }
        if (numero.length == 10 || numero.length == 11) {
            numero = "55$numero"
        }

        if (numero.length < 12) {
            Toast.makeText(this, "Número de WhatsApp inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero")).apply {
                setPackage("com.whatsapp")
            }
            try {
                startActivity(whatsappIntent)
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero")))
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarExclusao(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("Excluir cliente")
            .setMessage("Deseja realmente excluir ${cliente.nome}?")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("EXCLUIR") { _, _ ->
                val clientes = carregarClientes()
                clientes.removeAll { it.nome == cliente.nome }
                salvarClientes(clientes)
                onBackPressed()
            }
            .show()
    }
}
