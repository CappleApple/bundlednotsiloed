package com.cappleapple.bundlednotsiloed.client;

/** Minimal text-editing state shared by the compact inventory search controls. */
public final class BrowserSearchQuery {
    private final int maximumLength;
    private String value = "";
    private boolean allSelected;

    public BrowserSearchQuery(int maximumLength) {
        this.maximumLength = maximumLength;
    }

    public String value() { return value; }
    public boolean allSelected() { return allSelected && !value.isEmpty(); }

    public void selectAll() {
        allSelected = !value.isEmpty();
    }

    public void clearSelection() {
        allSelected = false;
    }

    public boolean clear() {
        if (value.isEmpty()) {
            allSelected = false;
            return false;
        }
        value = "";
        allSelected = false;
        return true;
    }

    public boolean backspace() {
        if (allSelected()) return clear();
        if (value.isEmpty()) return false;
        int previous = value.offsetByCodePoints(value.length(), -1);
        value = value.substring(0, previous);
        return true;
    }

    public boolean deleteSelection() {
        return allSelected() && clear();
    }

    public boolean append(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) return false;
        String appended = new String(Character.toChars(codePoint));
        String base = allSelected() ? "" : value;
        if (base.length() + appended.length() > maximumLength) return false;
        value = base + appended;
        allSelected = false;
        return true;
    }
}
