package dev.suresh

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.table.Borders.ALL
import com.github.ajalt.mordant.table.Borders.NONE
import com.github.ajalt.mordant.table.Borders.TOM_BOTTOM
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

fun main() {

  val t = Terminal(hyperlinks = true, interactive = true)

  val fgBg = red on green
  t.println(fgBg("---- START -----"))
  t.info("Hello World")
  t.muted("Muted!")
  t.danger("Danger!")
  t.success("Success!!")
  t.warning("Warning :) ")
  t.println("https://www.google.com")

  t.println((green + bold + underline)("This is green bold"))
  t.println("The foreground ${cyan.bg("color will stay the")} same")
  t.println(rgb("#b4eeb4")("This will get downsampled on terminals that don't support truecolor"))
  t.println("${red("red")} ${white("white")} and ${blue("blue")}")

  t.println(
      table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        align = RIGHT
        tableBorders = NONE
        column(0) {
          align = LEFT
          cellBorders = ALL
          style = magenta
        }
        column(3) {
          cellBorders = ALL
          style = magenta
        }
        header {
          style(magenta, bold = true)
          row("", "Projected Cost", "Actual Cost", "Difference")
        }
        body {
          rowStyles(blue, brightBlue)
          cellBorders = TOM_BOTTOM
          row("Food", "$400", "$200", "$200")
          row("Data", "$100", "$150", "-$50")
          row("Rent", "$800", "$800", "$0")
          row("Candles", "$0", "$3,600", "-$3,600")
          row("Utility", "$145", "$150", "-$5")
        }
        footer {
          style(bold = true)
          row {
            cell("Subtotal")
            cell("$-3,455") { columnSpan = 3 }
          }
        }
        captionBottom("Budget courtesy @dril")
      })

  t.print(fgBg("---- END -----"))
}
