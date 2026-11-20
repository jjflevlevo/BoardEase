package boardinghouse.model;

public class Room {
    private int roomId;
    private String roomNo;
    private int capacity;
    private double monthlyRent;

    public Room(int roomId, String roomNo, int capacity, double monthlyRent) {
        this.roomId = roomId;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.monthlyRent = monthlyRent;
    }

    public int getRoomId() { return roomId; }
    public String getRoomNo() { return roomNo; }
    public int getCapacity() { return capacity; }
    public double getMonthlyRent() { return monthlyRent; }
}