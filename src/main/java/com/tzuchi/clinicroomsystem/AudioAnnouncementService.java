package com.tzuchi.clinicroomsystem;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.application.Platform;
import java.io.File;
import java.util.*;

public class AudioAnnouncementService {
    private static final String BASE_PATH = "C:\\Users\\tina_\\Desktop\\TzuChiAudio";
    private Queue<AudioItem> audioQueue = new LinkedList<>();
    private boolean isPlaying = false;
    private MediaPlayer currentPlayer;
    private double playbackSpeed = 1.3;

    private static final double TRIM_END_THAI = 0.2;
    private static final double TRIM_END_ENGLISH = 0.8;
    private static final double GAP_THAI = 0;
    private static final double GAP_ENGLISH = 0;
    private static final double GAP_BETWEEN_LANGUAGES = 1.0;

    private static class AudioItem {
        MediaPlayer player;
        boolean isThai;
        boolean isFirst;

        AudioItem(MediaPlayer player, boolean isThai, boolean isFirst) {
            this.player = player;
            this.isThai = isThai;
            this.isFirst = isFirst;
        }
    }

    public void announceNumber(String patientId) {
        String category = String.valueOf(patientId.charAt(0));
        String number = patientId.substring(1);

        // Queue Thai announcement
        List<String> thaiSequence = new ArrayList<>();
        thaiSequence.add(String.format("%s\\Thai-word\\queue number.mp3", BASE_PATH));
        thaiSequence.add(String.format("%s\\Category\\Category%s.mp3", BASE_PATH, category));
        addThaiNumberToSequence(thaiSequence, number);
        thaiSequence.add(String.format("%s\\Thai-word\\please come to the.mp3", BASE_PATH));
        thaiSequence.add(String.format("%s\\Thai-word\\clinic.mp3", BASE_PATH));
        thaiSequence.add(String.format("%s\\Thai-word\\room.mp3", BASE_PATH));
        queueAudioSequence(thaiSequence, true);

        // Queue English announcement
        List<String> englishSequence = new ArrayList<>();
        englishSequence.add(String.format("%s\\Words\\queue number.mp3", BASE_PATH));
        englishSequence.add(String.format("%s\\Category\\Category%s.mp3", BASE_PATH, category));
        addEnglishNumberToSequence(englishSequence, number);
        englishSequence.add(String.format("%s\\Words\\please come to the.mp3", BASE_PATH));
        englishSequence.add(String.format("%s\\Words\\clinic.mp3", BASE_PATH));
        englishSequence.add(String.format("%s\\Words\\room.mp3", BASE_PATH));
        queueAudioSequence(englishSequence, false);

        if (!isPlaying) {
            playNextInQueue();
        }
    }

    private void addThaiNumberToSequence(List<String> sequence, String number) {
        int num = Integer.parseInt(number);
        if (num > 300) {
            System.err.println("Number too large: " + num);
            return;
        }

        // Handle hundreds
        if (num >= 100) {
            sequence.add(String.format("%s\\Thai-number\\%d.mp3", BASE_PATH, (num / 100 * 100)));
            num %= 100;
        }

        // Handle remaining number
        if (num > 0) {
            if (num == 11 || num == 21 || num == 31 || num == 41 ||
                    num == 51 || num == 61 || num == 71 || num == 81 || num == 91) {
                // Use special pronunciation for x1 numbers
                sequence.add(String.format("%s\\Thai-number\\%d.mp3", BASE_PATH, num));
            } else {
                // Split into tens and ones
                int tens = (num / 10) * 10;
                int ones = num % 10;

                // Add tens if it exists
                if (tens > 0) {
                    sequence.add(String.format("%s\\Thai-number\\%d.mp3", BASE_PATH, tens));
                }

                // Add ones if it exists
                if (ones > 0) {
                    sequence.add(String.format("%s\\Thai-number\\%d.mp3", BASE_PATH, ones));
                }
            }
        }
    }

    private void addEnglishNumberToSequence(List<String> sequence, String number) {
        int num = Integer.parseInt(number);
        if (num > 300) {
            System.err.println("Number too large: " + num);
            return;
        }

        if (num >= 100) {
            sequence.add(String.format("%s\\Number\\%d.mp3", BASE_PATH, (num / 100 * 100)));
            num %= 100;
        }

        if (num > 20) {
            sequence.add(String.format("%s\\Number\\%d.mp3", BASE_PATH, (num / 10 * 10)));
            if (num % 10 > 0) {
                sequence.add(String.format("%s\\Number\\%d.mp3", BASE_PATH, (num % 10)));
            }
        } else if (num > 0) {
            sequence.add(String.format("%s\\Number\\%d.mp3", BASE_PATH, num));
        }
    }

    private void queueAudioSequence(List<String> audioFiles, boolean isThai) {
        boolean isFirstFile = true;
        for (String audioPath : audioFiles) {
            try {
                File audioFile = new File(audioPath);
                if (audioFile.exists()) {
                    Media media = new Media(audioFile.toURI().toString());
                    MediaPlayer player = new MediaPlayer(media);
                    player.setRate(playbackSpeed);

                    player.setOnReady(() -> {
                        Duration totalDuration = media.getDuration();
                        double trimEnd = isThai ? TRIM_END_THAI : TRIM_END_ENGLISH;
                        player.setStopTime(totalDuration.subtract(Duration.seconds(trimEnd)));
                    });

                    audioQueue.offer(new AudioItem(player, isThai, isFirstFile));
                } else {
                    System.err.println("Audio file not found: " + audioPath);
                }
            } catch (Exception e) {
                System.err.println("Error loading audio file: " + audioPath);
                e.printStackTrace();
            }
            isFirstFile = false;
        }
    }

    private void playNextInQueue() {
        if (audioQueue.isEmpty()) {
            isPlaying = false;
            return;
        }

        isPlaying = true;
        AudioItem audioItem = audioQueue.poll();
        currentPlayer = audioItem.player;

        currentPlayer.setOnEndOfMedia(() -> {
            currentPlayer.dispose();
            Timer timer = new Timer(true);

            double gap;
            if (audioItem.isFirst && !audioItem.isThai) {
                gap = GAP_BETWEEN_LANGUAGES;
            } else {
                gap = audioItem.isThai ? GAP_THAI : GAP_ENGLISH;
            }

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> playNextInQueue());
                }
            }, (long)(gap * 1000));
        });

        currentPlayer.setOnError(() -> {
            System.err.println("Error playing audio: " + currentPlayer.getError());
            currentPlayer.dispose();
            playNextInQueue();
        });

        currentPlayer.play();
    }

    public void setPlaybackSpeed(double speed) {
        if (speed > 0) {
            playbackSpeed = speed;
            if (currentPlayer != null) {
                currentPlayer.setRate(speed);
            }
        }
    }

    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
        }
        audioQueue.clear();
        isPlaying = false;
    }
}