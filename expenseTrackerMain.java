import java.io.*;
import java.util.*;
import java.util.stream.*;

// ============================================================
//  EXPENSE TRACKER — DS & Algorithms Showcase
//  Each CO is annotated at the point where it is demonstrated.
// ============================================================

public class expenseTrackerMain {

    // ── Core application state ──────────────────────────────
    static Scanner sc = new Scanner(System.in);

    // CO5 / CO2: ArrayList is the primary LINEAR data structure used to store Expense ADTs.
    //            ArrayList gives O(1) amortized append and O(n) search — ideal for an
    //            unbounded, ordered collection of expenses.
    static ArrayList<Expense> expenses = new ArrayList<>();

    static String currentUser = "";
    static String EXPENSE_FILE = "";
    static final String USER_FILE = "users.txt";

    static double monthlyBudget = 0;
    static double savingsGoal   = 0;
    static double monthlyIncome = 0;

    static final List<String> categories = Arrays.asList(
            "Food", "Travel", "Bills", "Education",
            "Health", "Entertainment", "Other");

    // ── CO2: Singly Linked List (custom implementation) ─────
    // Demonstrates an alternative linear container with O(1) head insert
    // and O(n) traversal — used for the audit / undo log.
    static class Node {
        Expense data;
        Node next;
        Node(Expense data) { this.data = data; }
    }

    static Node auditHead = null; // head of the singly linked list

    /** CO2: Insert at head — O(1) time */
    static void auditPush(Expense e) {
        Node n = new Node(e);
        n.next = auditHead;
        auditHead = n;
    }

    /** CO2: Traverse the singly linked list — O(n) time */
    static void printAuditLog() {
        System.out.println("----- Audit Log (recent first) -----");
        Node cur = auditHead;
        while (cur != null) {
            System.out.println("  " + cur.data);
            cur = cur.next;
        }
    }

    // ── CO3: Stack (array-based) for undo functionality ─────
    // Last-In-First-Out semantics perfectly model "undo last action".
    // ArrayDeque is Java's recommended stack — O(1) push/pop.
    // CO4: Java Collections — Deque interface used here
    static Deque<Expense> undoStack = new ArrayDeque<>();

    /** CO3: Push deleted expense onto undo stack — O(1) */
    static void undoPush(Expense e) { undoStack.push(e); }

    /** CO3: Pop from undo stack to restore last deleted expense — O(1) */
    static void undoDelete() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        Expense restored = undoStack.pop();
        expenses.add(restored);
        System.out.println("Restored: " + restored);
    }

    // ── CO3: Priority Queue (Min-Heap) for top-expense queries ──
    // CO1: Heap insert O(log n), extract-min O(log n) — more efficient
    //      than sorting the full list O(n log n) when only top-k needed.
    // CO4: Java Collections — PriorityQueue used here
    static PriorityQueue<Expense> minHeap =
            new PriorityQueue<>(Comparator.comparingDouble(Expense::getAmount));

    /** CO3: Rebuild the heap from current expenses — O(n log n) */
    static void rebuildHeap() {
        minHeap.clear();
        minHeap.addAll(expenses);
    }

    /** CO3: Show the k cheapest expenses using the min-heap */
    static void showCheapestExpenses(int k) {
        rebuildHeap();
        System.out.println("----- " + k + " Cheapest Expenses -----");
        // CO1: Each poll is O(log n); total O(k log n)
        PriorityQueue<Expense> tmp = new PriorityQueue<>(minHeap);
        for (int i = 0; i < k && !tmp.isEmpty(); i++)
            System.out.println("  " + tmp.poll());
    }

    // ── CO3: Circular Queue (array-based) for recent-5 expenses ─
    // Demonstrates a fixed-size circular buffer — O(1) enqueue/dequeue.
    static final int RECENT_CAP = 5;
    static Expense[] recentRing = new Expense[RECENT_CAP];
    static int ringHead = 0, ringTail = 0, ringSize = 0;

    /** CO3: Enqueue into circular queue — O(1) */
    static void recentEnqueue(Expense e) {
        if (ringSize == RECENT_CAP) ringHead = (ringHead + 1) % RECENT_CAP; // overwrite oldest
        else ringSize++;
        recentRing[ringTail] = e;
        ringTail = (ringTail + 1) % RECENT_CAP;
    }

    /** CO3: Display recent expenses from the circular queue — O(RECENT_CAP) */
    static void showRecentExpenses() {
        System.out.println("----- Recent (up to 5) Expenses -----");
        int idx = ringHead;
        for (int i = 0; i < ringSize; i++) {
            System.out.println("  " + recentRing[idx]);
            idx = (idx + 1) % RECENT_CAP;
        }
    }

    // ── CO4: Hash Table — category → running total ───────────
    // HashMap gives O(1) average lookup/update — far better than
    // rescanning the full ArrayList (O(n)) for each category query.
    // CO1: Amortized O(1) per update; O(n) total for all expenses.
    static HashMap<String, Double> categoryTotals = new HashMap<>();

    /** CO4: Update hash table when a new expense is added — O(1) avg */
    static void updateCategoryTotals(Expense e) {
        categoryTotals.merge(e.getCategory(), e.getAmount(), Double::sum);
    }

    /** CO4: Update hash table when an expense is removed — O(1) avg */
    static void decrementCategoryTotal(Expense e) {
        categoryTotals.merge(e.getCategory(), -e.getAmount(), Double::sum);
    }

    // ═══════════════════════════════════════════════════════════
    //  SEARCHING — CO1 Big-O analysis in comments
    // ═══════════════════════════════════════════════════════════

    /**
     * CO1: Linear Search — O(n) time, O(1) space.
     * Scans each expense sequentially; no pre-sorting required.
     * Use when the list is unsorted or when only one result is needed.
     */
    static Expense linearSearchByDate(String date) {
        for (Expense e : expenses)
            if (e.getDate().equals(date)) return e;
        return null;
    }

    /**
     * CO1: Binary Search — O(log n) time, O(1) space.
     * Requires expenses sorted by amount first — O(n log n) pre-sort.
     * Justified when repeated range queries are needed on large datasets.
     */
    static int binarySearchByAmount(List<Expense> sorted, double target) {
        int lo = 0, hi = sorted.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            double midAmt = sorted.get(mid).getAmount();
            if (midAmt == target) return mid;
            else if (midAmt < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1; // not found
    }

    // ═══════════════════════════════════════════════════════════
    //  SORTING — CO1 multiple algorithms with Big-O justification
    // ═══════════════════════════════════════════════════════════

    /**
     * CO1: Bubble Sort — O(n²) worst/avg, O(n) best (already sorted).
     * Included for educational comparison; not recommended for large n.
     */
    static void bubbleSort(List<Expense> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++)
            for (int j = 0; j < n - i - 1; j++)
                if (list.get(j).getAmount() > list.get(j + 1).getAmount()) {
                    Expense tmp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, tmp);
                }
    }

    /**
     * CO1: Selection Sort — O(n²) all cases, O(1) extra space.
     * Fewer swaps than bubble sort; useful when write cost is high.
     */
    static void selectionSort(List<Expense> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++)
                if (list.get(j).getAmount() < list.get(minIdx).getAmount())
                    minIdx = j;
            Expense tmp = list.get(minIdx);
            list.set(minIdx, list.get(i));
            list.set(i, tmp);
        }
    }

    /**
     * CO1: Insertion Sort — O(n²) worst, O(n) best.
     * Efficient for small or nearly-sorted lists; stable sort.
     */
    static void insertionSort(List<Expense> list) {
        for (int i = 1; i < list.size(); i++) {
            Expense key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).getAmount() > key.getAmount()) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    /**
     * CO1: Merge Sort — O(n log n) all cases, O(n) extra space.
     * Stable, divide-and-conquer. Preferred for large expense lists.
     * Recurrence: T(n) = 2T(n/2) + O(n)  →  Θ(n log n) by Master Theorem.
     */
    static List<Expense> mergeSort(List<Expense> list) {
        if (list.size() <= 1) return list;
        int mid = list.size() / 2;
        List<Expense> left  = mergeSort(new ArrayList<>(list.subList(0, mid)));
        List<Expense> right = mergeSort(new ArrayList<>(list.subList(mid, list.size())));
        return merge(left, right);
    }

    static List<Expense> merge(List<Expense> l, List<Expense> r) {
        List<Expense> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < l.size() && j < r.size()) {
            if (l.get(i).getAmount() <= r.get(j).getAmount()) result.add(l.get(i++));
            else result.add(r.get(j++));
        }
        while (i < l.size()) result.add(l.get(i++));
        while (j < r.size()) result.add(r.get(j++));
        return result;
    }

    /**
     * CO1: Quick Sort — O(n log n) avg, O(n²) worst, O(log n) stack space.
     * In-place; cache-friendly. Fastest in practice for random data.
     * Recurrence (avg): T(n) = 2T(n/2) + O(n)  →  Θ(n log n).
     */
    static void quickSort(List<Expense> list, int lo, int hi) {
        if (lo < hi) {
            int p = partition(list, lo, hi);
            quickSort(list, lo, p - 1);
            quickSort(list, p + 1, hi);
        }
    }

    static int partition(List<Expense> list, int lo, int hi) {
        double pivot = list.get(hi).getAmount();
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (list.get(j).getAmount() <= pivot) {
                i++;
                Expense tmp = list.get(i); list.set(i, list.get(j)); list.set(j, tmp);
            }
        }
        Expense tmp = list.get(i + 1); list.set(i + 1, list.get(hi)); list.set(hi, tmp);
        return i + 1;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("===== WELCOME =====");
        System.out.println("1. Sign Up");
        System.out.println("2. Login");
        int option = sc.nextInt();
        sc.nextLine();

        if (option == 1) signUp();
        if (!login()) return;

        EXPENSE_FILE = currentUser + "_expenses.txt";
        loadExpenses();

        // CO4: Rebuild hash map from loaded expenses — O(n) avg
        for (Expense e : expenses) updateCategoryTotals(e);

        int choice;
        do {
            System.out.println("\n===== EXPENSE TRACKER =====");
            System.out.println(" 1. Add Expense");
            System.out.println(" 2. Monthly Summary");
            System.out.println(" 3. Set Monthly Budget");
            System.out.println(" 4. Category-wise Spending");
            System.out.println(" 5. Edit Expense");
            System.out.println(" 6. Delete Expense");
            System.out.println(" 7. Search");
            System.out.println(" 8. Monthly Report");
            System.out.println(" 9. Savings Goal");
            System.out.println("10. Sort Expenses");
            System.out.println("11. Show Recent Expenses");
            System.out.println("12. Show Cheapest Expenses");
            System.out.println("13. Undo Last Delete");
            System.out.println("14. Show Audit Log");
            System.out.println("15. Exit");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case  1 -> addExpense();
                case  2 -> monthlySummary();
                case  3 -> setBudget();
                case  4 -> categoryWiseSpending();
                case  5 -> editExpense();
                case  6 -> deleteExpense();
                case  7 -> searchMenu();
                case  8 -> monthlyReport();
                case  9 -> savingsFeature();
                case 10 -> sortMenu();
                case 11 -> showRecentExpenses();          // CO3 circular queue
                case 12 -> { System.out.print("How many cheapest to show? "); showCheapestExpenses(sc.nextInt()); sc.nextLine(); } // CO3 heap
                case 13 -> undoDelete();                  // CO3 stack
                case 14 -> printAuditLog();               // CO2 linked list
                case 15 -> saveExpenses();
            }
        } while (choice != 15);
    }

    // ═══════════════════════════════════════════════════════════
    //  USER METHODS
    // ═══════════════════════════════════════════════════════════

    // CO5: File-based persistence — stores users in a flat text file
    static void signUp() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USER_FILE, true))) {
            System.out.print("Username: ");
            String name = sc.nextLine();
            System.out.print("Password: ");
            String pass = sc.nextLine();
            pw.println(name + "," + pass);
            System.out.println("Sign Up Successful!");
        } catch (IOException e) {
            System.out.println("Error.");
        }
    }

    // CO1: Login scan — O(n) linear search through user records
    static boolean login() {
        System.out.print("Username: ");
        String name = sc.nextLine();
        System.out.print("Password: ");
        String pass = sc.nextLine();

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d[0].equals(name) && d[1].equals(pass)) {
                    currentUser = name;
                    System.out.println("Login Success!");
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("User file not found.");
        }

        System.out.println("Login Failed.");
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPENSE METHODS
    // ═══════════════════════════════════════════════════════════

    // CO5: Core application feature — Add expense to all data structures
    // CO2: ArrayList insert — O(1) amortized
    // CO4: HashMap update — O(1) avg
    // CO2: Linked list head insert — O(1)
    // CO3: Circular queue enqueue — O(1)
    static void addExpense() {
        System.out.print("Date (DD-MM-YYYY): ");
        String date = sc.nextLine();

        System.out.println("Choose Category:");
        for (int i = 0; i < categories.size(); i++)
            System.out.println((i + 1) + "." + categories.get(i));

        int c = sc.nextInt();
        sc.nextLine();
        String category = categories.get(c - 1);

        System.out.print("Amount: ");
        double amount = sc.nextDouble();
        sc.nextLine();

        System.out.print("Description: ");
        String desc = sc.nextLine();

        Expense e = new Expense(date, category, amount, desc);

        // CO2: ArrayList append — O(1) amortized
        expenses.add(e);

        // CO4: O(1) avg hash map update
        updateCategoryTotals(e);

        // CO2: O(1) linked list head insert (audit trail)
        auditPush(e);

        // CO3: O(1) circular queue enqueue (recent-5 view)
        recentEnqueue(e);

        System.out.println("Expense Added!");
        checkBudgetWarning();
    }

    // CO5: Monthly summary feature — O(n) scan
    static void monthlySummary() {
        System.out.print("Enter month (MM-YYYY): ");
        String month = sc.nextLine();
        double total = 0;

        // CO1: O(n) linear scan — no better option without date-indexed structure
        for (Expense e : expenses)
            if (e.getDate().contains(month))
                total += e.getAmount();

        System.out.println("Total: " + total);
        System.out.println("Remaining Budget: " + (monthlyBudget - total));
    }

    // CO4: Category-wise spending using the HashMap — O(n) to build, O(1) per lookup
    static void categoryWiseSpending() {
        // CO4: Use the maintained HashMap instead of rescanning ArrayList
        System.out.println("----- Category-wise Spending -----");
        categoryTotals.forEach((k, v) -> System.out.printf("  %-15s : %.2f%n", k, v));
    }

    static void setBudget() {
        System.out.print("Enter Monthly Budget: ");
        monthlyBudget = sc.nextDouble();
        sc.nextLine();
    }

    // CO1: O(n) sum using Java Stream — Θ(n) regardless of input
    static void checkBudgetWarning() {
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        if (monthlyBudget > 0 && total > monthlyBudget)
            System.out.println("⚠ WARNING: Budget Exceeded!");
    }

    // CO5: Edit feature — O(1) index access on ArrayList
    static void editExpense() {
        displayExpenses();
        System.out.print("Enter index to edit: ");
        int i = sc.nextInt();
        sc.nextLine();

        if (i >= 0 && i < expenses.size()) {
            Expense old = expenses.get(i);

            // CO4: Remove old amount from category hash map before update
            decrementCategoryTotal(old);

            System.out.print("New Amount: ");
            old.setAmount(sc.nextDouble());
            sc.nextLine();

            System.out.print("New Category: ");
            old.setCategory(sc.nextLine());

            // CO4: Re-add updated values to hash map
            updateCategoryTotals(old);

            System.out.println("Updated!");
        }
    }

    // CO5: Delete feature
    // CO3: Deleted expense pushed to undo stack — O(1)
    static void deleteExpense() {
        System.out.println("1.Delete by Index");
        System.out.println("2.Delete by Date");
        int ch = sc.nextInt();
        sc.nextLine();

        if (ch == 1) {
            displayExpenses();
            System.out.print("Index: ");
            int i = sc.nextInt();
            sc.nextLine();
            if (i >= 0 && i < expenses.size()) {
                Expense removed = expenses.remove(i);
                // CO4: Sync hash map on delete — O(1) avg
                decrementCategoryTotal(removed);
                // CO3: Push to undo stack — O(1)
                undoPush(removed);
                System.out.println("Deleted. (Undo available via menu option 13)");
            }
        } else {
            System.out.print("Enter Date: ");
            String d = sc.nextLine();
            // CO1: removeIf is O(n) linear scan
            List<Expense> toRemove = expenses.stream()
                    .filter(e -> e.getDate().equals(d))
                    .collect(Collectors.toList());
            toRemove.forEach(e -> {
                decrementCategoryTotal(e); // CO4
                undoPush(e);               // CO3
            });
            expenses.removeIf(e -> e.getDate().equals(d));
        }
    }

    // CO5: Search feature demonstrating multiple search strategies
    static void searchMenu() {
        System.out.println("1.Search by Date       [Linear Search  O(n)]");
        System.out.println("2.Search by Category   [Hash Map       O(1) avg]");
        System.out.println("3.Search by Amount Range [Linear O(n)]");
        System.out.println("4.Binary Search by Amount [O(log n) after sort]");
        int ch = sc.nextInt();
        sc.nextLine();

        if (ch == 1) {
            // CO1: Linear search by date — O(n)
            System.out.print("Date: ");
            String d = sc.nextLine();
            expenses.stream().filter(e -> e.getDate().equals(d))
                    .forEach(System.out::println);

        } else if (ch == 2) {
            // CO4: Hash map category lookup — O(1) avg for total, O(n) for listing items
            System.out.print("Category: ");
            String cat = sc.nextLine();
            System.out.println("Category Total (from HashMap): "
                    + categoryTotals.getOrDefault(cat, 0.0));
            expenses.stream().filter(e -> e.getCategory().equalsIgnoreCase(cat))
                    .forEach(System.out::println);

        } else if (ch == 3) {
            // CO1: Linear range scan — O(n)
            System.out.print("Min: ");
            double min = sc.nextDouble();
            System.out.print("Max: ");
            double max = sc.nextDouble();
            sc.nextLine();
            expenses.stream()
                    .filter(e -> e.getAmount() >= min && e.getAmount() <= max)
                    .forEach(System.out::println);

        } else {
            // CO1: Binary search after merge sort — O(n log n) sort + O(log n) search
            System.out.print("Target Amount: ");
            double target = sc.nextDouble();
            sc.nextLine();
            List<Expense> sorted = mergeSort(new ArrayList<>(expenses));
            int idx = binarySearchByAmount(sorted, target);
            if (idx == -1) System.out.println("Not found.");
            else System.out.println("Found: " + sorted.get(idx));
        }
    }

    // CO5: Monthly report — aggregates all stats in O(n)
    static void monthlyReport() {
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        System.out.println("----- Monthly Report -----");
        System.out.println("Total Transactions : " + expenses.size());
        System.out.println("Total Expense      : " + total);
        categoryWiseSpending(); // CO4: O(categories) using hash map
    }

    // CO5: Savings feature
    static void savingsFeature() {
        System.out.print("Enter Monthly Income: ");
        monthlyIncome = sc.nextDouble();
        System.out.print("Enter Savings Goal: ");
        savingsGoal = sc.nextDouble();
        sc.nextLine();

        // CO1: O(n) stream reduce
        double totalExpense = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double savings = monthlyIncome - totalExpense;

        System.out.println("Savings This Month: " + savings);
        if (savings >= savingsGoal) System.out.println("🎉 Goal Achieved!");
        else System.out.println("You need " + (savingsGoal - savings) + " more.");
    }

    // ─────────────────────────────────────────────────────────
    // CO1: Sort Menu — compare algorithms empirically
    // ─────────────────────────────────────────────────────────
    static void sortMenu() {
        System.out.println("Choose Sort Algorithm:");
        System.out.println("1. Bubble Sort   O(n²)");
        System.out.println("2. Selection Sort O(n²)");
        System.out.println("3. Insertion Sort O(n²) / O(n) best");
        System.out.println("4. Merge Sort    O(n log n)  ← recommended");
        System.out.println("5. Quick Sort    O(n log n) avg");
        int ch = sc.nextInt();
        sc.nextLine();

        List<Expense> copy = new ArrayList<>(expenses);

        // CO1: Empirical time measurement — wall-clock comparison of algorithms
        long start = System.nanoTime();

        switch (ch) {
            case 1 -> bubbleSort(copy);
            case 2 -> selectionSort(copy);
            case 3 -> insertionSort(copy);
            case 4 -> copy = mergeSort(copy);
            case 5 -> { quickSort(copy, 0, copy.size() - 1); }
            default -> { System.out.println("Invalid."); return; }
        }

        long elapsed = System.nanoTime() - start;
        System.out.printf("Sort complete in %d ns (n=%d)%n", elapsed, copy.size());
        System.out.println("----- Sorted Expenses (by Amount) -----");
        copy.forEach(e -> System.out.println("  " + e));
    }

    // CO2: Display helper — O(n) traversal of ArrayList
    static void displayExpenses() {
        for (int i = 0; i < expenses.size(); i++)
            System.out.println(i + " -> " + expenses.get(i));
    }

    // CO5: File persistence — save all expenses on exit
    static void saveExpenses() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EXPENSE_FILE))) {
            // CO2: O(n) traversal of ArrayList for serialization
            for (Expense e : expenses)
                pw.println(e);
        } catch (IOException e) {
            System.out.println("Save Error.");
        }
        System.out.println("Data saved");
    }

    // CO5: File persistence — load expenses on startup
    static void loadExpenses() {
        File file = new File(EXPENSE_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                Expense e = new Expense(d[0], d[1], Double.parseDouble(d[2]), d[3]);
                expenses.add(e);              // CO2: ArrayList append O(1) amortized
                auditPush(e);                 // CO2: Linked list head insert O(1)
                recentEnqueue(e);             // CO3: Circular queue O(1)
            }
        } catch (IOException e) {
            System.out.println("Load Error.");
        }
    }
}