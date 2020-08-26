package minesweeper

import kotlin.random.Random
import java.util.*

const val DEBUG = true

// TODO Убрать русские комменты
// TODO Подумать над try / catch
// TODO Coords вообще нужен?
// TODO Вместо DEBUG -> Logger

class Coords(val x: Int, val y: Int)  // uses for storing of coordinates of cells

object PlayingField {
    private const val size = 9
    private val field = Array(size) { CharArray(size) { '.' } }
    private val marks = Array(size) { CharArray(size) { '.' } }
    private var mines: Int = 0
    private var markedMines: Int = 0
    private var minesLeft: Int = 0
    var steppedOnMine: Boolean = false
    private var firstMove: Boolean = true
    private var emptiesNotExplored: Int = 0

    fun init(minesForGame: Int) {
        mines = minesForGame
        minesLeft = mines
    }

    fun placeMines(minesForGame: Int) {
        var minesLeftToPlace = minesForGame
        do {
            val x = Random.nextInt(size)
            val y = Random.nextInt(size)
            if (field[x][y] != 'X') {
                field[x][y] = 'X'
                minesLeftToPlace--
                // calculate neighbor cells
                for (i in maxOf(0, x - 1)..minOf(size - 1, x + 1))
                    for (j in maxOf(0, y - 1)..minOf(size - 1, y + 1))
                        when (field[i][j]) {
                            '.' -> field[i][j] = '1'
                            in '1'..'9' -> field[i][j]++
                        }
            }
        } while (minesLeftToPlace > 0)

        // calculate empties
        emptiesNotExplored = 0
        for (i in 0 until size)
            for (j in 0 until size)
                if (field[i][j] == '.') println("i = $i j = $j empties = ${++emptiesNotExplored}")
        println("EMPTIES: $emptiesNotExplored")
    }

    fun setCell(y: Int, x: Int, cmd: String) {
        when {
            cmd == "free" && firstMove -> { // the first cell explored with the free command cannot be a mine; it should always be empty.
                while (field[x][y] == 'X') {
                    field[x][y] = '.'
                    placeMines(1)
                    if (DEBUG) println("переставляем мины")
                }
                marks[x][y] = '/'
                firstMove = false
                if (DEBUG) { println("Вот поле после возможной перестановки"); printField(showMines = true, isDebug = true) }
                recalculate(x, y)
            }
            cmd == "free" && field[x][y] == 'X' -> {
                steppedOnMine = true
            }
            cmd == "free" -> { // marks = {. or 1..9} && field <> 'X'
                if (marks[x][y] == '*') markedMines-- // free/explore cell previously marked as mine
                marks[x][y] = '/'
                recalculate(x, y)
            }
            cmd == "mine" && marks[x][y] == '*' -> { // delete own mark, marks = {*, . or 1..9} && field <> 'X'
                marks[x][y] = '.' // initial state
                markedMines--
                if (field[x][y] == 'X') minesLeft++
            }
            cmd == "mine" && marks[x][y] in '1'..'8' -> { // error move
                println("There is a number here!")
            }
            cmd == "mine" && field[x][y] == '.' -> { // marked empty cell
                marks[x][y] = '*'
                markedMines++
            }
            cmd == "mine" && field[x][y] == 'X' -> { // marked a bomb
                marks[x][y] = '*'
                markedMines++
                minesLeft--
            }
        }
    }

    private fun recalculate(x_init: Int, y_init: Int) {
        // x,y - is empty cell. find new empty surrounding cells and display them at marks array

        var x = x_init
        var y = y_init

        val a = mutableListOf<Coords>() // storing of founded empty cells
        do {
            if (!a.isNullOrEmpty()) {
                x = a[a.lastIndex].x  // переименовать в a[0].x
                y = a[a.lastIndex].y
                a.removeAt(a.lastIndex)
//                print("Остались: ")
//                a.forEach { e -> print("(${e.x + 1},${e.y + 1}) ") }
//                println()
            }

//            println("Взяли клетку x=${x+1},y=${y+1}")
//            println("значение empties: $emptiesNotExplored")
            printField(showMines = true, isDebug = true)

            for (i in maxOf(0, x - 1)..minOf(size - 1, x + 1))
                for (j in maxOf(0, y - 1)..minOf(size - 1, y + 1))
                    if (field[i][j] == '.') {
                        print("((${i + 1},${j + 1})=${field[i][j]} ")
                        field[i][j] = '/'
                        marks[i][j] = '/'
                        a.add(Coords(i, j))
                        emptiesNotExplored--
                        println("($i,$j) emptiesNotExplored: $emptiesNotExplored")
                    }
            println()
//            print("после того как прошли цикл, нашли следующие пустые клетки и поставили на них А: ")
//            a.forEach { e -> print("(${e.x + 1},${e.y + 1}) ") }
//            println()

        } while (!a.isNullOrEmpty())
    }


    fun gameIsOver(): Boolean {
        return (minesLeft == 0 && markedMines == mines) || steppedOnMine || emptiesNotExplored == 0
    }

    fun printField(showMines: Boolean, isDebug: Boolean) {
        if (isDebug) println("DEBUG MODE") else println("GAME MODE")
        //println(" │123456789│")
        print(" │")
        print(CharArray(size) { i -> '1' + i })
        println("│")
        println("—│${"—".repeat(size)}│")
        for (i in 0 until size) {
            print("${i + 1}│")
            for (j in 0 until size)
                print(when {
                    marks[i][j] != '.' -> marks[i][j]
                    field[i][j] == 'X' && !showMines -> '.'
                    field[i][j] == 'X' && showMines -> 'X'
                    isDebug -> field[i][j]
                    else -> '.'
                })
            println("│")
        }
        println("—│${"—".repeat(size)}│")
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    print("How many mines do you want on the field? > ")
    val minesForGame = scanner.nextInt()
    if (!(minesForGame in 1..80)) {
        println("Number of mines must be between 1..80")
        return
    }

    PlayingField.init(minesForGame)
    PlayingField.placeMines(minesForGame)

//    PlayingField.printField(showMines = true, isDebug = true)
//    PlayingField.printField(showMines = true, isDebug = false)

    do {
        PlayingField.printField(showMines = true, isDebug = true)
        PlayingField.printField(showMines = false, isDebug = false)
        print("Set/unset mine marks or claim a cell as free: > ")
        PlayingField.setCell(scanner.nextInt() - 1, scanner.nextInt() - 1, scanner.next())
    } while (!PlayingField.gameIsOver())

    PlayingField.printField(showMines = true, isDebug = true)
    PlayingField.printField(showMines = true, isDebug = false)
    if (PlayingField.steppedOnMine) {
        println("You stepped on a mine and failed!")
    } else println("Congratulations! You found all the mines!")
}

// Legend
//  . as unexplored cells
//  / as explored free cells without mines around it
//  Numbers from 1 to 8 as explored free cells with 1 to 8 mines around them, respectively
//  X as mines
//  * as unexplored marked cells