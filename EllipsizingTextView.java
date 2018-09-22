package com.glidetest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EllipsizingTextView extends TextView {
    private static final String ELLIPSIS = "\u2026";
    private static final Pattern DEFAULT_END_PUNCTUATION = Pattern.compile("[\\.,\u2026;\\:\\s]*$", Pattern.DOTALL);

    public interface EllipsizeListener {
        void ellipsizeStateChanged(boolean ellipsized);
    }

    private final List<EllipsizeListener> ellipsizeListeners = new ArrayList<EllipsizeListener>();
    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private CharSequence fullText;
    private int maxLines;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;
    private CharSequence oldText = "";
    /**
     * The end punctuation which will be removed when appending #ELLIPSIS.
     */
    private Pattern endPunctuationPattern;

    public EllipsizingTextView(Context context) {
        this(context, null);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setEllipsize(null);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.maxLines });
        setMaxLines(a.getInt(0, Integer.MAX_VALUE));
        a.recycle();
        setEndPunctuationPattern(DEFAULT_END_PUNCTUATION);
    }

    public void setEndPunctuationPattern(Pattern pattern) {
        this.endPunctuationPattern = pattern;
    }

    public void addEllipsizeListener(EllipsizeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        ellipsizeListeners.add(listener);
    }

    public void removeEllipsizeListener(EllipsizeListener listener) {
        ellipsizeListeners.remove(listener);
    }

    public boolean isEllipsized() {
        return isEllipsized;
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this.maxLines = maxLines;
        isStale = true;
    }

    @SuppressLint("Override")
    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        oldText = text;
    }

    public boolean ellipsizingLastFullyVisibleLine() {
        return maxLines == Integer.MAX_VALUE;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before,
                                 int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text;
            isStale = true;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (ellipsizingLastFullyVisibleLine()) {
            isStale = true;
        }else {
            setText(fullText);
        }
    }

    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        if (ellipsizingLastFullyVisibleLine()) {
            isStale = true;
        }else {
            setText(fullText);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStale) {
            resetText();
        }
        super.onDraw(canvas);
    }

    private void resetText() {
        CharSequence workingText = fullText;
        boolean ellipsized = false;
        Layout layout = createWorkingLayout(workingText);
        int linesCount = getLinesCount();
        if (layout.getLineCount() > linesCount) {
            // We have more lines of text than we are allowed to display.
            try {
                log("fulltext_____" + fullText);
                log("linesCount_____" + linesCount);
                CharSequence firstLine = fullText.subSequence(0, layout.getLineEnd(0));
                if (linesCount == 1) {
                    log("firstLine_____" + firstLine);
                    int end = firstLine.length();
                    workingText = firstLine + ELLIPSIS;
                    while (createWorkingLayout(firstLine.subSequence(0, end) + ELLIPSIS).getLineCount() > 1) {
                        --end;
                        if (end < 0) {
                            break;
                        }
                        workingText = firstLine.subSequence(0, end) + ELLIPSIS;
                    }
                } else {
                    firstLine = fullText.subSequence(0, layout.getLineEnd(linesCount - 2));
                    log("firstLine_____" + firstLine);
                    //取出最后两行字符串
                    //取出最后两行字符串
                    CharSequence lastTwoLine = fullText.subSequence(layout.getLineStart(layout.getLineCount() - linesCount), fullText.length()).toString();
                    log("lastTwoLine_____" + lastTwoLine);
                    //算出倒数第二行的中间截止位置index
                    int lastHalfStart = layout.getLineStart(layout.getLineCount() - linesCount);
                    int lastHalfEnd = layout.getLineEnd(layout.getLineCount() - linesCount);
                    CharSequence lastHalfLine = fullText.subSequence(lastHalfStart, lastHalfStart + (lastHalfEnd - lastHalfStart) / 2);
                    log("lastHalfLine_____" + lastHalfLine);
                    CharSequence lastEndHalfLine = fullText.subSequence(lastHalfStart + (lastHalfEnd - lastHalfStart) / 2, fullText.length());
                    log("lastEndHalfLine_____" + lastEndHalfLine);
                    int start = 0;
                    workingText = firstLine + lastHalfLine.toString() + ELLIPSIS + lastEndHalfLine;
                    while (createWorkingLayout(firstLine + lastHalfLine.toString() + ELLIPSIS + lastEndHalfLine.subSequence(start, lastEndHalfLine.length())).getLineCount() > linesCount) {
                        start += 1;
                        if (start > lastEndHalfLine.length()) {
                            break;
                        }
                        workingText = firstLine + lastHalfLine.toString() + ELLIPSIS + lastEndHalfLine.subSequence(start, lastEndHalfLine.length());
                    }
                    log("finally workingText_____" + workingText);
                }
            } catch (Exception e) {
                e.printStackTrace();
                workingText = fullText;
            }
            ellipsized = true;
        }
        if (!workingText.equals(getText())) {
            programmaticChange = true;
            try {
                setText(workingText);
            } finally {
                programmaticChange = false;
            }
        }
        isStale = false;
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
            for (EllipsizeListener listener : ellipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized);
            }
        }
    }

    private void log(String msg) {
        Log.d(EllipsizingTextView.class.getSimpleName(), msg);
    }

    /**
     * Get how many lines of text we are allowed to display.
     */
    private int getLinesCount() {
        if (ellipsizingLastFullyVisibleLine()) {
            int fullyVisibleLinesCount = getFullyVisibleLinesCount();
            if (fullyVisibleLinesCount == -1) {
                return 1;
            } else {
                return fullyVisibleLinesCount;
            }
        } else {
            return maxLines;
        }
    }

    /**
     * Get how many lines of text we can display so their full height is visible.
     */
    private int getFullyVisibleLinesCount() {
        Layout layout = createWorkingLayout("");
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int lineHeight = layout.getLineBottom(0);
        return height / lineHeight;
    }

    private Layout createWorkingLayout(CharSequence workingText) {
        return new StaticLayout(workingText, getPaint(),
                getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight(),
                Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier,
                lineAdditionalVerticalPadding, false /* includepad */);
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt where) {
        // Ellipsize settings are not respected
    }
}