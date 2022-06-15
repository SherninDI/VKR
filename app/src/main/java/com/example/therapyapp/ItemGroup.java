package com.example.therapyapp;

public class ItemGroup {
    private int groupId;
    private String groupName;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ItemGroup(int id, String name){
        this.groupId = id;
        this.groupName = name;
    }
}
