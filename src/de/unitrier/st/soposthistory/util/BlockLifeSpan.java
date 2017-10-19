package de.unitrier.st.soposthistory.util;

import java.util.LinkedList;

public class BlockLifeSpan extends LinkedList<BlockLifeSpanSnapshot> {

    public enum Type{ codeblock, textblock }

    private Type type;


    public static int getNumberOfSnapshots(LinkedList<BlockLifeSpan> blockLifeSpans){
        int numberOfSnapshots = 0;

        for (BlockLifeSpan blockLifeSpan : blockLifeSpans) {
            numberOfSnapshots += blockLifeSpan.size();
        }

        return numberOfSnapshots;
    }
    
    public static String printLinkedListOfLifeSpans(LinkedList<BlockLifeSpan> blockLifeSpans){

        StringBuilder sb = new StringBuilder();

        sb.append("number of snapshots: ");
        sb.append(getNumberOfSnapshots(blockLifeSpans));
        sb.append("\n");

        for (BlockLifeSpan blockLifeSpan : blockLifeSpans) {
            sb.append(blockLifeSpan);
        }

        return  sb.toString();
    }

    public BlockLifeSpan(Type type){
        this.type = type;
    }

    public Type getType(){
        return type;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(BlockLifeSpanSnapshot snapshot : this){
            sb.append(snapshot);
        }
        return type + ": " + sb + "\n";
    }

}
