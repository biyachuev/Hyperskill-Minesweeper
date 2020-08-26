package minesweeper

import java.util.*
import kotlin.math.abs
import kotlin.random.Random

const val ANSI_RESET = "\u001B[0m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_BLUE = "\u001B[34m"

const val DEBUG_LVL = 0 // 100 - print uncovered field, 200 - verbose info

class Cell {
    var visible: Boolean = false
    var mine: Boolean = false
    var minesAround: Int = 0
    var marked: Boolean = false
}

object Board {
    private const val size = 9
    private val field: Array<Array<Cell>> = Array(size) { Array(size) { Cell() } }
    private var freeCellFirstMove: Boolean = true
    private var minesLeft: Int = 0

    enum class GameStatuses { Continue, SteppedOnMine, ExploredAllEmpties, FoundAllMines, Error }
    private var gmStatus: GameStatuses = GameStatuses.Continue
    fun gmStatus(): GameStatuses = gmStatus

    override fun toString(): String {
        var res = ""
        res += " │" + CharArray(size) { i -> '1' + i }.joinToString("") + "│" + "\n"
        res += "—│${"—".repeat(size)}│" + "\n"

        for (i in field.indices) {
            res += "${i + 1}│"
            for (el in field[i])
                res += (when {
                    DEBUG_LVL >= 100 -> {
                        when {
                            el.marked -> ANSI_BLUE + "*" + ANSI_RESET
                            el.mine -> ANSI_RED + "X" + ANSI_RESET
                            el.visible && el.minesAround == 0 -> ANSI_BLUE + "/" + ANSI_RESET
                            el.visible -> ANSI_BLUE + el.minesAround + ANSI_RESET
                            else -> el.minesAround
                        }
                    }
                    el.mine && gmStatus == GameStatuses.SteppedOnMine -> {
                        "X" //ANSI_RED + "X" + ANSI_RESET
                    }
                    el.marked -> {
                        "*" //ANSI_BLUE + "*" + ANSI_RESET
                    }
                    el.visible && el.minesAround == 0 -> {
                        "/" //ANSI_GREEN + "/" + ANSI_RESET
                    }
                    !el.visible -> {
                        '.'
                    }
                    else -> {
                        el.minesAround //ANSI_GREEN + el.minesAround + ANSI_RESET
                    }
                })
            res += "│" + "\n"
        }
        res += "—│${"—".repeat(size)}│" + "\n"
        return res
    }

    fun setNumberOfMines(num: Int) {
        if (num !in 1..90) {
            println("Number of mines must be between 1..90")
            gmStatus = GameStatuses.Error
        } else
            minesLeft = num
    }

    private fun outBounds(x: Int, y: Int): Boolean {
        return x < 0 || y < 0 || x >= size || y >= size
    }

    private fun calcMinesAround(x: Int, y: Int): Int { // a priori (x,y) is not a mine
        var temp = 0
        for (i in -1..1)
            for (j in -1..1)
                if (!outBounds(x + i, y + j)) {
                    if (field[x + i][y + j].mine) temp++
                }
        return temp
    }

    fun executeCmd(y: Int, x: Int, cmd: String) { // y,x
        when (cmd) {
            "free" -> freeCell(x, y)
            "mine" -> markCell(x, y)
        }
    }

    private fun placeMines(x_restricted: Int, y_restricted: Int) {
        var minesLeftToPlace = minesLeft
        if (DEBUG_LVL == 200) println("-- placeMines")
        do {
            val x = Random.nextInt(size)
            val y = Random.nextInt(size)
            if (DEBUG_LVL == 200) println("-- ($x,$y)")
            if (!field[x][y].mine && !(abs(x - x_restricted) <= 1 && abs(y - y_restricted) <= 1)) {
                field[x][y].mine = true
                minesLeftToPlace--

                for (i in -1..1)
                    for (j in -1..1)
                        if (!outBounds(x + i, y + j)) {
                            field[x + i][y + j].minesAround++
                        }
            }
        } while (minesLeftToPlace > 0)
        if (DEBUG_LVL == 200) println(Board)
    }

    private fun reveal(x: Int, y: Int) {
        field[x][y].visible = true
        for (i in -1..1)
            for (j in -1..1)
                if (!outBounds(x + i, y + j)) {
                    if (DEBUG_LVL == 200) println("-- (${x + i},${y + j}) = ${calcMinesAround(x + i, y + j)}")
                    if (calcMinesAround(x + i, y + j) == 0 && !(i == 0 && j == 0) && !field[x + i][y + j].visible) {
                        reveal(x + i, y + j)
                    } else if (!field[x + i][y + j].mine) {
                        field[x + i][y + j].visible = true
                        field[x + i][y + j].marked = false // especially for 8th test -> cell was marked as mine, but it is a neighbour of empty cell
                    }
                }
    }

    private fun freeCell(x: Int, y: Int) {
        if (DEBUG_LVL == 200) println("-- freeCell")
        when {
            freeCellFirstMove -> {
                freeCellFirstMove = false
                placeMines(x, y)
                freeCell(x, y)
            }
            field[x][y].mine -> {
                gmStatus = GameStatuses.SteppedOnMine
                return
            }
            !field[x][y].marked && !field[x][y].visible && calcMinesAround(x,y) > 0 -> {
                field[x][y].visible = true // point a cell with a number
            }
            !field[x][y].marked && !field[x][y].visible -> {
                reveal(x,y)
            }
        }
        gmStatus = GameStatuses.ExploredAllEmpties    // if explored all empties -> game ends. Lets check it
        for (i in field.indices)
            for (el in field[i])
                if (!el.visible && !el.mine) { // if there is a cell which is not visible(explored) and not a mine - than game continues
                    gmStatus = GameStatuses.Continue
                    break
                }
    }

    private fun markCell(x: Int, y: Int) {
        when {
            field[x][y].marked -> {
                field[x][y].marked = false
            }
            field[x][y].visible -> {
                println(ANSI_RED + "Already explored!" + ANSI_RESET)
                return
            }
            else -> {
                field[x][y].marked = true
            }
        }

        gmStatus = GameStatuses.FoundAllMines     // found all mines -> game ends. Lets check it
        for (i in field.indices)
            for (el in field[i])
                if (el.marked xor el.mine) {    // if there is a cell which is not visible(explored) and not a mine -> than game continues
                    gmStatus = GameStatuses.Continue
                    break
                }
    }

}

fun main() {
    val scanner = Scanner(System.`in`)
    print("How many mines do you want on the field? > ")
    Board.setNumberOfMines(scanner.nextInt())
    if (Board.gmStatus() != Board.GameStatuses.Error) print(Board) else return

    do {
        print("Set/unset mine marks or claim a cell as free: > ")
        Board.executeCmd(scanner.nextInt() - 1, scanner.nextInt() - 1, scanner.next()) // e.g. "1 1 free" -> (0, 0, "free")
        print(Board)
    } while (Board.gmStatus() == Board.GameStatuses.Continue)

    println(when (Board.gmStatus()) {
        Board.GameStatuses.SteppedOnMine -> ANSI_RED + "You stepped on a mine and failed!" + ANSI_RESET
        else -> ANSI_GREEN + "Congratulations! You found all the mines!" + ANSI_RESET
    })
}