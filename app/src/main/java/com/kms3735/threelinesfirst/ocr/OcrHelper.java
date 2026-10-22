package com.kms3735.threelinesfirst.ocr;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** ML Kit 한국어 글자 인식. 사진은 기기 밖으로 나가지 않는다. */
public class OcrHelper {

    /** 이보다 적게 읽히면 요약으로 넘어가지 않고 다시 찍게 한다. */
    public static final int MIN_CHARS = 30;

    public interface Callback {
        void onSuccess(String text);
        void onFailure(Exception e);
    }

    private final TextRecognizer recognizer =
            TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

    public void recognize(Context context, File photo, Callback callback) {
        InputImage image;
        try {
            // fromFilePath는 사진의 회전(EXIF)까지 반영한다
            image = InputImage.fromFilePath(context, Uri.fromFile(photo));
        } catch (IOException e) {
            callback.onFailure(e);
            return;
        }
        recognizer.process(image)
                .addOnSuccessListener(result -> callback.onSuccess(inReadingOrder(result)))
                .addOnFailureListener(callback::onFailure);
    }

    private static String inReadingOrder(Text result) {
        List<ReadingOrder.Line> lines = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect box = line.getBoundingBox();
                if (box == null) continue;
                lines.add(new ReadingOrder.Line(line.getText(), box.left, box.top, box.right, box.bottom));
            }
        }
        return lines.isEmpty() ? result.getText() : ReadingOrder.arrange(lines);
    }

    public static boolean isEnough(String text) {
        return text.replaceAll("\\s", "").length() >= MIN_CHARS;
    }

    public void close() {
        recognizer.close();
    }
}
