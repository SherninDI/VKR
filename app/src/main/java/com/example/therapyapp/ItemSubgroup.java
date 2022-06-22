package com.example.therapyapp;


public class ItemSubgroup {
    private int subGroupId;
    private int groupId;
    private String subGroupName;
    private String subGroupFormatValue;
    private int valueId;
    private int Value;

    public int getValueId() {
        return valueId;
    }

    public void setValueId(int valueId) {
        this.valueId = valueId;
    }

    public int getSubGroupId() {
        return subGroupId;
    }

    public void setSubGroupId(int subGroupId) {
        this.subGroupId = subGroupId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getSubGroupName() {
        return subGroupName;
    }

    public void setSubGroupName(String subGroupName) {
        this.subGroupName = subGroupName;
    }

    public String getSubGroupValue() {   return subGroupFormatValue;   }

    public void setSubGroupValue(String subGroupFormatValue) {
        this.subGroupFormatValue = subGroupFormatValue;
    }

    public int getValue() {   return Value;   }

    public void setValue(int Value) {
        this.Value = Value;
    }

    public ItemSubgroup(int value_id, int subgroup_id,int group_id, String name, int value){
        this.valueId = value_id;
        this.subGroupId = subgroup_id;
        this.groupId = group_id;
        this.subGroupName = name;
        this.Value = value;


        if ((value > 0) && (value < 1000)) {
            String val = "F"+String.valueOf(value);
            subGroupFormatValue = val;
        } else if((value > 4000) && (value < 5000)) {
            value %= 1000;
            String val = "A"+String.valueOf(value);
            subGroupFormatValue = val;
        }
    }

}
