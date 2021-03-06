package com.group2.blackjack.Activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.group2.blackjack.Entities.Card
import com.group2.blackjack.R
import android.widget.RelativeLayout

import com.group2.blackjack.Callbacks.GameOverCallback
import android.content.Intent
import android.support.v7.app.AlertDialog
import com.group2.blackjack.Enums.EndGameState
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.group2.blackjack.Callbacks.UpdateCardSumCallback
import com.group2.blackjack.Game.Game
import android.R.string.cancel
import android.content.DialogInterface
import android.text.InputType
import android.support.v4.widget.SearchViewCompat.setInputType
import android.widget.EditText
import com.group2.blackjack.Communication.RestClient


class MainActivity : AppCompatActivity(), GameOverCallback, UpdateCardSumCallback{

    private lateinit var splitButton : Button
    private lateinit var hitButton : Button
    private lateinit var submitButton : Button
    private lateinit var balance : TextView
    private lateinit var game : Game
    private lateinit var startButton : Button
    private lateinit var standButton : Button
    private lateinit var cardLayout : RelativeLayout
    private lateinit var dealerLayout : RelativeLayout
    private var numbersOfPlayerHits : Int = 0
    private lateinit var backView : ImageView
    private lateinit var seekBar : SeekBar
    private lateinit var betText : TextView
    private var currBet : Float = 0f
    private lateinit var playerSum : TextView
    private lateinit var dealerSum : TextView
    private val restClient = RestClient()
    private var inputName = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        splitButton = findViewById(R.id.splitButton) as Button
        hitButton = findViewById(R.id.hitButton) as Button
        standButton = findViewById(R.id.standButton) as Button
        balance = findViewById(R.id.balanceText) as TextView
        startButton = findViewById(R.id.startButton) as Button
        standButton = findViewById(R.id.standButton) as Button
        submitButton = findViewById(R.id.submitButton) as Button
        cardLayout = findViewById(R.id.playerCardsLayout) as RelativeLayout
        dealerLayout = findViewById(R.id.dealerCardsLayout) as RelativeLayout
        seekBar = findViewById(R.id.seekBar) as SeekBar
        betText = findViewById(R.id.betText) as TextView
        playerSum = findViewById(R.id.playerSum) as TextView
        dealerSum = findViewById(R.id.dealerSum) as TextView

        buttonAction()

        hitButton.isEnabled = false
        standButton.isEnabled = false
        game = Game(balance, this, this)
        game.initGame()

        //game.startRound()
        //continue round

    }

    override fun endGame(winner : EndGameState){
        val cardString = game.table.dealer[1].toString()
        val id = resources.getIdentifier(cardString, "drawable", packageName)
        backView.setImageResource(id)
        hitButton.isEnabled = false
        standButton.isEnabled = false

        when (winner) {
            EndGameState.PLAYER -> {
                println("Player won-----")
                alertBox("You won!")
            }
            EndGameState.DEALER -> {
                println("Dealer won----")
                alertBox("You lost!")
            }
            else -> {
                println("Push----")
                alertBox("Push")
            }
        }
    }

    override fun updateSum(player: Int, dealer: Int, showDealer: Boolean) {
        playerSum.text = player.toString()
        if(showDealer){
            dealerSum.text = dealer.toString()
        }
    }

    private fun alertBox(message : String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setTitle("Round over")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, { _, _ ->
                })
                //.setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    private fun submitDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Send highscore")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            inputName = input.text.toString()
            restClient.postScore(inputName, game.table.money)
            game.table.money = 500}
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


    private fun buttonAction() {
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var maxBet = (balance.text).toString().toInt()
                currBet = ( maxBet * ((progress*10) / 100.0f))
                if(progress <= 1) {
                    currBet = ( maxBet * ((progress*1) / 100.0f))
                }
                betText.text = (currBet).toString()
            }
        })


        splitButton.setOnClickListener{
            val intent = Intent(this, HighscoreActivity::class.java)
            startActivity(intent)
        }
        submitButton.setOnClickListener{
            submitDialog()
        }

        standButton.setOnClickListener{
            if (!game.roundOver){
                var card = game.stand()
                println("Dealerdraw: " + card)
                var numbersOfDealerHits = 1
                if(card != null){
                    numbersOfDealerHits++
                    setImageToScreen(game.table.dealer, numbersOfDealerHits, dealerLayout, false)
                }
                while(card != null){
                    card = game.stand()
                    println("Dealerdraw " + card)
                    numbersOfDealerHits++
                    if (card != null){
                        setImageToScreen(game.table.dealer, numbersOfDealerHits, dealerLayout, false)
                    }
                }
            }
        }

        //HIT
        hitButton.setOnClickListener {
            if (!game.roundOver){
                val playerDraw = game.playerHit() // can be null
                println(playerDraw)
                if (playerDraw != null) {
                    numbersOfPlayerHits++
                    setImageToScreen(game.table.player, numbersOfPlayerHits, cardLayout, false)
                }
            }
        }

        //START
        startButton.setOnClickListener{
            init()
            numbersOfPlayerHits = 1

            game.startRound(currBet.toInt())
            val table = game.table

            //player cards
            for(i in 0..1){
                setImageToScreen(table.player, i, cardLayout,false)
            }
            //dealer cards
            setImageToScreen(table.dealer, 0, dealerLayout,false)
            setImageToScreen(table.dealer, 1, dealerLayout,true)
        }
    }

    private fun init(){
        cardLayout.removeAllViews()
        dealerLayout.removeAllViews()
        playerSum.text = ""
        dealerSum.text = ""
        hitButton.isEnabled = true
        standButton.isEnabled = true
    }

    private fun setImageToScreen(cards : List<Card>, i: Int, layout: RelativeLayout, showBackground: Boolean) {
        val cardString = cards[i].toString()

        val imgView = ImageView(this)
        val id = resources.getIdentifier(cardString, "drawable", packageName)

        if(showBackground){
            imgView.setImageResource(R.drawable.b0)
            backView = imgView
        }
        else {
            imgView.setImageResource(id)
        }

        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.leftMargin = (i*70)
        imgView.layoutParams = params

        layout.addView(imgView, i)
    }
}
