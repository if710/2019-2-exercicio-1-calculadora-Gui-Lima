package br.ufpe.cin.android.calculadora

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener {

    final val MATH_RESULT = "result"
    final val MATH_CURRENT = "current"

    private lateinit var edit_text : EditText
    private lateinit var text_info : TextView

    //Pega o saveState caso exista
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_constraint)
        edit_text = findViewById(R.id.text_calc)
        text_info = findViewById(R.id.text_info)
        val edit_textPreviousText = savedInstanceState?.get(MATH_CURRENT) ?: ""
        edit_text.setText(edit_textPreviousText.toString())
        val text_infoPreviousText = savedInstanceState?.get(MATH_RESULT) ?: ""
        text_info.setText(text_infoPreviousText.toString())

        setListenersOnButtons()
    }

    //Como usar a função:
    // eval("2+2") == 4.0
    // eval("2+3*4") = 14.0
    // eval("(2+3)*4") = 20.0
    //Fonte: https://stackoverflow.com/a/26227947
    //Fiz algumas mudanças para invés de jogar exceção, mostrar o dialog
    fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch: Char = ' '
            fun nextChar() {
                val size = str.length
                ch = if ((++pos < size)) str.get(pos) else (-1).toChar()
            }

            fun eat(charToEat: Char): Boolean {
                while (ch == ' ') nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) {
                    makeDialog("Caractere inesperado: " + ch)
                    return -0.0
                }
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            // | number | functionName factor | factor `^` factor
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'))
                        x += parseTerm() // adição
                    else if (eat('-'))
                        x -= parseTerm() // subtração
                    else
                        return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'))
                        x *= parseFactor() // multiplicação
                    else if (eat('/'))
                        x /= parseFactor() // divisão
                    else
                        return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor() // + unário
                if (eat('-')) return -parseFactor() // - unário
                var x: Double
                val startPos = this.pos
                if (eat('(')) { // parênteses
                    x = parseExpression()
                    eat(')')
                } else if ((ch in '0'..'9') || ch == '.') { // números
                    while ((ch in '0'..'9') || ch == '.') nextChar()
                    try {x = java.lang.Double.parseDouble(str.substring(startPos, this.pos))}
                    catch (e:Exception){
                        return 0.0
                    }
                } else if (ch in 'a'..'z') { // funções
                    while (ch in 'a'..'z') nextChar()
                    val func = str.substring(startPos, this.pos)
                    x = parseFactor()
                    if (func == "sqrt")
                        x = Math.sqrt(x)
                    else if (func == "sin")
                        x = Math.sin(Math.toRadians(x))
                    else if (func == "cos")
                        x = Math.cos(Math.toRadians(x))
                    else if (func == "tan")
                        x = Math.tan(Math.toRadians(x))
                    else{
                        makeDialog("Função desconhecida: " + func)
                        return -0.0
                    }
                } else {
                    makeDialog("Caractere inesperado: " + ch.toChar())
                    return -0.0
                }
                if (eat('^')) x = Math.pow(x, parseFactor()) // potência
                return x
            }
        }.parse()
    }

    //Edita texto que mostra os números digitados de acordo com número digitado
    override fun onClick(p0: View?) {
        var buttonId = p0?.id
        var clickedText = getChoosenButton(buttonId)
        var text = if (clickedText.equals("")) "" else edit_text.text.toString().plus(clickedText)
        edit_text.setText(text)
    }

    //Lida com mudança de config.
    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString(MATH_RESULT, text_info.text.toString())
        savedInstanceState.putString(MATH_CURRENT, edit_text.text.toString())
        super.onSaveInstanceState(savedInstanceState)
    }

    //Recebe o id do botão apertado e decide oque fazer
    fun getChoosenButton(buttonId: Int?) : String{
        return when(buttonId) {
            R.id.btn_0 -> "0"
            R.id.btn_1 -> "1"
            R.id.btn_2 -> "2"
            R.id.btn_3 -> "3"
            R.id.btn_4 -> "4"
            R.id.btn_5 -> "5"
            R.id.btn_6 -> "6"
            R.id.btn_7 -> "7"
            R.id.btn_8 -> "8"
            R.id.btn_9 -> "9"
            R.id.btn_Equal -> calculaSoma()
            R.id.btn_Multiply -> "*"
            R.id.btn_Divide -> "/"
            R.id.btn_Add -> "+"
            R.id.btn_Subtract -> "-"
            R.id.btn_LParen -> "("
            R.id.btn_RParen -> ")"
            R.id.btn_Clear -> ""
            R.id.btn_Dot -> "."
            R.id.btn_Power -> "^"
            else -> "masoq"
        }
    }

    //Editar o textView do resultado da conta
    private fun calculaSoma(): String {
        text_info.setText(eval(edit_text.text.toString()).toString())
        return ""
    }

    //Mostra a caixa de diálogo de erro
    private fun makeDialog(message:String){
        var alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Alerta") // O Titulo da notificação
        alertDialog.setMessage(message)
        alertDialog.setView(R.layout.dialog_somethingwentwrong_fragment)

        alertDialog.setPositiveButton("OK", { _, _ ->

            Toast.makeText(this, "Sua conta foi ignorada e resultado zerado", Toast.LENGTH_LONG).show()
        })

        alertDialog.show()
    }

    //Coloca os listeners em todos os botões
    private fun setListenersOnButtons() {
        (findViewById(R.id.btn_0) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_1) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_2) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_3) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_4) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_5) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_6) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_7) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_8) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_9) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Clear) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Power) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_RParen) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_LParen) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Equal) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Dot) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Add) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Divide) as Button).setOnClickListener(this)
        (findViewById(R.id.btn_Multiply) as Button).setOnClickListener(this)
    }

}
