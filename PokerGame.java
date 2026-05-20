import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PokerGameGUI extends JFrame {
    private final JLabel chipsLabel = new JLabel("Chips: 500");
    private final JLabel messageLabel = new JLabel("Click Deal to start.");
    private final JLabel[] playerCards = new JLabel[5];
    private final JLabel[] dealerCards = new JLabel[5];
    private final JCheckBox[] holdBoxes = new JCheckBox[5];

    private final JButton dealButton = new JButton("Deal");
    private final JButton drawButton = new JButton("Draw");
    private final JSpinner betSpinner = new JSpinner(new SpinnerNumberModel(25, 5, 500, 5));

    private int chips = 500;
    private int bet = 25;
    private Deck deck;
    private List<Card> playerHand;
    private List<Card> dealerHand;

    public PokerGameGUI() {
        setTitle("Poker Game");
        setSize(750, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        main.setBackground(new Color(22, 95, 60));
        add(main);

        JPanel top = new JPanel(new GridLayout(1, 3));
        top.setOpaque(false);
        chipsLabel.setForeground(Color.WHITE);
        messageLabel.setForeground(Color.WHITE);
        top.add(chipsLabel);
        top.add(new JLabel("Java Poker", SwingConstants.CENTER) {{
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 22));
        }});
        top.add(messageLabel);
        main.add(top, BorderLayout.NORTH);

        JPanel table = new JPanel(new GridLayout(2, 1, 10, 20));
        table.setOpaque(false);
        table.add(createHandPanel("Dealer", dealerCards, null));
        table.add(createHandPanel("Player", playerCards, holdBoxes));
        main.add(table, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(new JLabel("Bet:") {{
            setForeground(Color.WHITE);
        }});
        controls.add(betSpinner);
        controls.add(dealButton);
        controls.add(drawButton);
        main.add(controls, BorderLayout.SOUTH);

        drawButton.setEnabled(false);

        dealButton.addActionListener(e -> deal());
        drawButton.addActionListener(e -> drawCards());
    }

    private JPanel createHandPanel(String title, JLabel[] cards, JCheckBox[] holds) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridLayout(1, 5, 10, 10));
        row.setOpaque(false);

        for (int i = 0; i < 5; i++) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);

            cards[i] = new JLabel("", SwingConstants.CENTER);
            cards[i].setFont(new Font("Arial", Font.BOLD, 24));
            cards[i].setOpaque(true);
            cards[i].setBackground(Color.WHITE);
            cards[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            slot.add(cards[i], BorderLayout.CENTER);

            if (holds != null) {
                holds[i] = new JCheckBox("Hold");
                holds[i].setHorizontalAlignment(SwingConstants.CENTER);
                holds[i].setOpaque(false);
                holds[i].setForeground(Color.WHITE);
                slot.add(holds[i], BorderLayout.SOUTH);
            }

            row.add(slot);
        }

        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private void deal() {
        bet = (int) betSpinner.getValue();

        if (bet <= 0 || bet > chips) {
            JOptionPane.showMessageDialog(this, "Invalid bet.");
            return;
        }

        chips -= bet;
        deck = new Deck();
        playerHand = new ArrayList<>();
        dealerHand = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
            holdBoxes[i].setSelected(false);
        }

        updatePlayerCards();
        hideDealerCards();

        chipsLabel.setText("Chips: " + chips);
        messageLabel.setText("Choose cards to hold, then click Draw.");

        dealButton.setEnabled(false);
        drawButton.setEnabled(true);
        betSpinner.setEnabled(false);
    }

    private void drawCards() {
        for (int i = 0; i < 5; i++) {
            if (!holdBoxes[i].isSelected()) {
                playerHand.set(i, deck.draw());
            }
        }

        dealerDraw();

        HandValue playerValue = HandEvaluator.evaluate(playerHand);
        HandValue dealerValue = HandEvaluator.evaluate(dealerHand);

        updatePlayerCards();
        updateDealerCards();

        int result = playerValue.compareTo(dealerValue);

        if (result > 0) {
            chips += bet * 2;
            messageLabel.setText("You win with " + playerValue.name + "!");
        } else if (result < 0) {
            messageLabel.setText("Dealer wins with " + dealerValue.name + ".");
        } else {
            chips += bet;
            messageLabel.setText("Tie! Both have " + playerValue.name + ".");
        }

        chipsLabel.setText("Chips: " + chips);

        if (chips <= 0) {
            messageLabel.setText("Game over. You are out of chips.");
            dealButton.setEnabled(false);
        } else {
            dealButton.setEnabled(true);
        }

        drawButton.setEnabled(false);
        betSpinner.setEnabled(true);
    }

    private void dealerDraw() {
        HandValue value = HandEvaluator.evaluate(dealerHand);

        if (value.rank >= 1) {
            return;
        }

        int highest = 0;
        for (int i = 1; i < dealerHand.size(); i++) {
            if (dealerHand.get(i).rank.value > dealerHand.get(highest).rank.value) {
                highest = i;
            }
        }

        for (int i = 0; i < dealerHand.size(); i++) {
            if (i != highest) {
                dealerHand.set(i, deck.draw());
            }
        }
    }

    private void updatePlayerCards() {
        for (int i = 0; i < 5; i++) {
            playerCards[i].setText(playerHand.get(i).toString());
        }
    }

    private void updateDealerCards() {
        for (int i = 0; i < 5; i++) {
            dealerCards[i].setText(dealerHand.get(i).toString());
        }
    }

    private void hideDealerCards() {
        for (int i = 0; i < 5; i++) {
            dealerCards[i].setText("?");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PokerGameGUI().setVisible(true));
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
    CLUBS("C"), DIAMONDS("D"), HEARTS("H"), SPADES("S");

    String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }
}

enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"),
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"),
    TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"),
    KING(13, "K"), ACE(14, "A");

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

        for (int i = 0; i < Math.min(tieBreakers.size(), other.tieBreakers.size()); i++) {
            if (!tieBreakers.get(i).equals(other.tieBreakers.get(i))) {
                return Integer.compare(tieBreakers.get(i), other.tieBreakers.get(i));
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
