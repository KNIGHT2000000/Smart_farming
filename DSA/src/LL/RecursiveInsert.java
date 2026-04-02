package LL;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class RecursiveInsert {
    public static void  display(Node head) {
        Node temp = head;
        while (temp != null) {
            System.out.print(temp.data + " ");
            temp = temp.next;
        }
        System.out.println();
    }



    public static Node insert(Node head, int data, int position) {
        //base case
        if (position == 0) {
            Node newNode = new Node(data);
            newNode.next = head;
            return newNode;
        }
        //recursive case
        head.next = insert(head.next, data, position - 1);
        return head;
    }

    public static void main(String[] args) {
   //insertion in ll using recursive approach
        Node head = new Node(1);
        head.next = new Node(2);
        head.next.next = new Node(3);
        head.next.next.next = new Node(4);

        int data = 5;
        int position = 2;
        head = insert(head, data, position);
        display(head);


    }
}