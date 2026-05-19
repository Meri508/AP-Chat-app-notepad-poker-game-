import java.util.*;

public class PokerGame {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int chips = 500;

        System.out.println("=== Java Poker Game ===");
        System.out.println("Five-card draw poker. You start with 500 chips.");

        while (chips > 0) {
            System.out.println("\nYour chips: " + chips);
            System.out.print("Enter bet: ");
            int bet = scanner.nextInt();

            if (bet <= 0 || bet > chips) {
                System.out.println("Invalid bet.");
                continue;
            }

            chips -= bet;

            Deck deck = new Deck();
            List<Card> player = new ArrayList<>();
            List<Card> dealer = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                player.add(deck.draw());
                dealer.add(deck.draw());
            }

            System.out.println("\nYour hand:");
            printHand(player);

            System.out.println("Enter card numbers to replace, separated by spaces.");
            System.out.println("Example: 1 3 5");
            System.out.println("Press Enter to keep all cards.");

            scanner.nextLine();
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                String[] parts = input.split("\\s+");
                Set<Integer> replace = new HashSet<>();

                for (String part : parts) {
                    try {
                        int index = Integer.parseInt(part);
                        if (index >= 1 && index <= 5) {
                            replace.add(index - 1);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                for (int index : replace) {
                    player.set(index, deck.draw());
                }
            }

            dealerDraw(dealer, deck);

            HandValue playerValue = HandEvaluator.evaluate(player);
            HandValue dealerValue = HandEvaluator.evaluate(dealer);

            System.out.println("\nYour final hand:");
            printHand(player);
            System.out.println("You have: " + playerValue.name);

            System.out.println("\nDealer hand:");
            printHand(dealer);
            System.out.println("Dealer has: " + dealerValue.name);

            int result = playerValue.compareTo(dealerValue);

            if (result > 0) {
                int winnings = bet * 2;
                chips += winnings;
                System.out.println("\nYou win " + winnings + " chips!");
            } else if (result < 0) {
                System.out.println("\nDealer wins. You lose your bet.");
            } else {
                chips += bet;
                System.out.println("\nIt's a tie. Your bet is returned.");
            }

            System.out.print("\nPlay another round? (y/n): ");
            String again = scanner.nextLine().trim().toLowerCase();

            if (!again.equals("y")) {
                break;
            }
        }

        System.out.println("\nGame over. Final chips: " + chips);
        scanner.close();
    }

    static void printHand(List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            System.out.println((i + 1) + ". " + hand.get(i));
        }
    }

    static void dealerDraw(List<Card> dealer, Deck deck) {
        HandValue value = HandEvaluator.evaluate(dealer);

        if (value.rank >= 1) {
            return;
        }

        int highestIndex = 0;
        for (int i = 1; i < dealer.size(); i++) {
            if (dealer.get(i).rank.value > dealer.get(highestIndex).rank.value) {
                highestIndex = i;
            }
        }

        for (int i = 0; i < dealer.size(); i++) {
            if (i != highestIndex) {
                dealer.set(i, deck.draw());
            }
        }
    }
}

class Card {
    Rank rank;
    Suit suit;

    Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public String toString() {
        return rank.label + suit.symbol;
    }
}

enum Suit {
    CLUBS("C"),
    DIAMONDS("D"),
    HEARTS("H"),
    SPADES("S");

    String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }
}

enum Rank {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A");

    int value;
    String label;

    Rank(int value, String label) {
        this.value = value;
        this.label = label;
    }
}

class Deck {
    private final List<Card> cards = new ArrayList<>();

    Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }

        Collections.shuffle(cards);
    }

    Card draw() {
        return cards.remove(cards.size() - 1);
    }
}

class HandValue implements Comparable<HandValue> {
    int rank;
    String name;
    List<Integer> tieBreakers;

    HandValue(int rank, String name, List<Integer> tieBreakers) {
        this.rank = rank;
        this.name = name;
        this.tieBreakers = tieBreakers;
    }

    public int compareTo(HandValue other) {
        if (this.rank != other.rank) {
            return Integer.compare(this.rank, other.rank);
        }

        for (int i = 0; i < Math.min(this.tieBreakers.size(), other.tieBreakers.size()); i++) {
            if (!this.tieBreakers.get(i).equals(other.tieBreakers.get(i))) {
                return Integer.compare(this.tieBreakers.get(i), other.tieBreakers.get(i));
            }
        }

        return 0;
    }
}

class HandEvaluator {
    static HandValue evaluate(List<Card> hand) {
        List<Integer> ranks = new ArrayList<>();
        Map<Integer, Integer> counts = new HashMap<>();

        boolean flush = true;
        Suit firstSuit = hand.get(0).suit;

        for (Card card : hand) {
            ranks.add(card.rank.value);
            counts.put(card.rank.value, counts.getOrDefault(card.rank.value, 0) + 1);

            if (card.suit != firstSuit) {
                flush = false;
            }
        }

        ranks.sort(Collections.reverseOrder());

        int straightHigh = getStraightHigh(ranks);
        boolean straight = straightHigh > 0;

        List<Map.Entry<Integer, Integer>> groups = new ArrayList<>(counts.entrySet());
        groups.sort((a, b) -> {
            if (!a.getValue().equals(b.getValue())) {
                return b.getValue() - a.getValue();
            }

            return b.getKey() - a.getKey();
        });

        if (straight && flush && straightHigh == 14) {
            return new HandValue(9, "Royal Flush", List.of(14));
        }

        if (straight && flush) {
            return new HandValue(8, "Straight Flush", List.of(straightHigh));
        }

        if (groups.get(0).getValue() == 4) {
            return new HandValue(7, "Four of a Kind", groupValues(groups));
        }

        if (groups.get(0).getValue() == 3 && groups.get(1).getValue() == 2) {
            return new HandValue(6, "Full House", groupValues(groups));
        }

        if (flush) {
            return new HandValue(5, "Flush", ranks);
        }

        if (straight) {
            return new HandValue(4, "Straight", List.of(straightHigh));
        }

        if (groups.get(0).getValue() == 3) {
            return new HandValue(3, "Three of a Kind", groupValues(groups));
        }

        if (groups.get(0).getValue() == 2 && groups.get(1).getValue() == 2) {
            return new HandValue(2, "Two Pair", groupValues(groups));
        }

        if (groups.get(0).getValue() == 2) {
            return new HandValue(1, "One Pair", groupValues(groups));
        }

        return new HandValue(0, "High Card", ranks);
    }

    static int getStraightHigh(List<Integer> ranks) {
        List<Integer> unique = new ArrayList<>();

        for (int rank : ranks) {
            if (!unique.contains(rank)) {
                unique.add(rank);
            }
        }

        if (unique.size() != 5) {
            return 0;
        }

        if (unique.equals(List.of(14, 5, 4, 3, 2))) {
            return 5;
        }

        int high = unique.get(0);

        for (int i = 1; i < unique.size(); i++) {
            if (unique.get(i) != high - i) {
                return 0;
            }
        }

        return high;
    }

    static List<Integer> groupValues(List<Map.Entry<Integer, Integer>> groups) {
        List<Integer> values = new ArrayList<>();

        for (Map.Entry<Integer, Integer> group : groups) {
            values.add(group.getKey());
        }

        return values;
    }
}
