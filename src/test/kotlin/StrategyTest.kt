import kotlin.test.assertEquals
import org.example.Adventure
import org.example.AdventureStrategy
import org.example.Difficulty
import org.junit.jupiter.api.Test

class StrategyTest {

  private val testAdventures =
      mapOf(
          1 to
              Adventure(
                  difficulty = Difficulty.DIFFICULT,
                  duration = 180,
                  experience = 100,
                  fightChance = 100,
                  gold = 1000,
                  questId = 1),
          2 to
              Adventure(
                  difficulty = Difficulty.DIFFICULT,
                  duration = 120,
                  experience = 70,
                  fightChance = 100,
                  gold = 1000,
                  questId = 2),
          3 to
              Adventure(
                  difficulty = Difficulty.DIFFICULT,
                  duration = 200,
                  experience = 90,
                  fightChance = 100,
                  gold = 4000,
                  questId = 3))

  @Test
  fun testDefaultStrategy() {
    val adventures = testAdventures.values.toList()

    val expectedOrder = listOf(testAdventures[1], testAdventures[3], testAdventures[2])

    val sortedAdventures =
        AdventureStrategy.MAX_VALUE.strategy.sortByStrategy(prioritizeGold = false, adventures)

    assertEquals(expectedOrder, sortedAdventures)
  }

  @Test
  fun testMaxPerMinStrategy() {
    val adventures = testAdventures.values.toList()

    val expectedOrder = listOf(testAdventures[2], testAdventures[1], testAdventures[3])

    val sortedAdventures =
        AdventureStrategy.MAX_PER_MIN.strategy.sortByStrategy(prioritizeGold = false, adventures)

    assertEquals(expectedOrder, sortedAdventures)
  }

  @Test
  fun testBestCombinedStrategy() {
    val adventures = testAdventures.values.toList()

    val expectedOrder = listOf(testAdventures[3], testAdventures[1], testAdventures[2])

    val sortedAdventures =
        AdventureStrategy.BEST_COMBINED.strategy.sortByStrategy(prioritizeGold = false, adventures)

    assertEquals(expectedOrder, sortedAdventures)
  }
}
