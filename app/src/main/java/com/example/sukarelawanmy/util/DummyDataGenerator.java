package com.example.sukarelawanmy.util;

import com.example.sukarelawanmy.model.Event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DummyDataGenerator {

    public static List<Event> generateDummyEvents() {
        List<Event> events = new ArrayList<>();

        String[] titles = {"Beach Cleanup", "Blood Donation Camp", "Food Drive", "Tree Plantation", "Charity Run"};
        String[] locations = {"Kuala Lumpur", "Penang", "Johor Bahru", "Malacca", "Kota Kinabalu"};
        String[] descriptions = {
                "Join us to clean the beach and save marine life.",
                "Donate blood and help save lives today.",
                "Distribute food to the needy in your community.",
                "Help plant trees and make the earth greener.",
                "Run for a cause to raise charity funds."
        };
        String[] imageUrls = {
                "https://picsum.photos/id/1011/500/300",
                "https://picsum.photos/200/300?grayscale",
                "https://picsum.photos/id/1015/500/300",
                "https://picsum.photos/200/300?grayscale",
                "https://picsum.photos/id/1018/500/300"
        };

        for (int i = 0; i < titles.length; i++) {
            Event event = new Event();
            event.setId("event_" + (i + 1));
            event.setTitle(titles[i]);
            event.setLocation(locations[i]);
            event.setDescription(descriptions[i]);

            // Set date to i days from now
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, i + 1);
            event.setDate(calendar.getTime());

            event.setImageUrl(imageUrls[i]);

            events.add(event);
        }

        return events;
    }
}
