package de.unitrier.st.soposthistory.util;

import org.hibernate.StatelessSession;

import java.util.List;

public class Util {
    public static void insertList(StatelessSession session, List list) {
        for (int i=0; i<list.size(); i++) {
            session.insert(list.get(i));
        }
    }

    public static void updateList(StatelessSession session, List list) {
        for (int i=0; i<list.size(); i++) {
            session.update(list.get(i));
        }
    }
}
