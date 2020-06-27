package com.camelcc.keyboard.en;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/*
 * File header layout is as follows:
 *
 * v |
 * e | MAGIC_NUMBER + version of the file format, 2 bytes.
 * r |
 * sion
 *
 * o |
 * p | not used, 2 bytes.
 * o |
 * nflags
 *
 * h |
 * e | size of the file header, 4bytes
 * a |   including the size of the magic number, the option flags and the header size
 * d |
 * ersize
 *
 */

/*
 * Node array (FusionDictionary.PtNodeArray) layout is as follows:
 *
 * n |
 * o | the number of PtNodes, 1 or 2 bytes.
 * d | 1 byte = bbbbbbbb match
 * e |   case 1xxxxxxx => xxxxxxx << 8 + next byte
 * c |   otherwise => bbbbbbbb
 * o |
 * unt
 *
 * n |
 * o | sequence of PtNodes,
 * d | the layout of each PtNode is described below.
 * e |
 * s
 */

/* Node (FusionDictionary.PtNode) layout is as follows:
 *   | CHILDREN_ADDRESS_TYPE  2 bits, 11          : FLAG_CHILDREN_ADDRESS_TYPE_THREEBYTES
 *   |                                10          : FLAG_CHILDREN_ADDRESS_TYPE_TWOBYTES
 * f |                                01          : FLAG_CHILDREN_ADDRESS_TYPE_ONEBYTE
 * l |                                00          : FLAG_CHILDREN_ADDRESS_TYPE_NOADDRESS
 * a | has several chars ?         1 bit, 1 = yes, 0 = no   : FLAG_HAS_MULTIPLE_CHARS
 * g | has a terminal ?            1 bit, 1 = yes, 0 = no   : FLAG_IS_TERMINAL
 *
 * s | has shortcut targets ?      1 bit, 1 = yes, 0 = no   : FLAG_HAS_SHORTCUT_TARGETS
 *   | has bigrams ?               1 bit, 1 = yes, 0 = no   : FLAG_HAS_BIGRAMS
 *   | is not a word ?             1 bit, 1 = yes, 0 = no   : FLAG_IS_NOT_A_WORD
 *   | is cached suggestions?      1 bit, 1 = yes, 0 = no   : FLAG_HAS_CACHED_SUGGESTIONS
 *
 * c | IF FLAG_HAS_MULTIPLE_CHARS
 * h |   char, char, char, char    n * (1 or 3 bytes) : use PtNodeInfo for i/o helpers
 * a |   end                       1 byte, = 0
 * r | ELSE
 * s |   char                      1 or 3 bytes
 *   | END
 *
 * f |
 * r | IF FLAG_IS_TERMINAL
 * e |   frequency                 1 byte
 * q |
 *
 * c |
 * h | children address, CHILDREN_ADDRESS_TYPE bytes
 * i | This address is relative to the position of this field.
 * l |
 * drenaddress
 *
 *   | IF FLAG_IS_TERMINAL && FLAG_HAS_CACHED_SUGGESTIONS
 *   | cached suggestions list
 *   | IF FLAG_IS_TERMINAL && FLAG_HAS_SHORTCUT_TARGETS
 *   | shortcut string list
 *
 * Char format is:
 * 1 byte = bbbbbbbb match
 * case 000xxxxx: xxxxx << 16 + next byte << 8 + next byte
 * else: if 00011111 (= 0x1F) : this is the terminator. This is a relevant choice because
 *       unicode code points range from 0 to 0x10FFFF, so any 3-byte value starting with
 *       00011111 would be outside unicode.
 * else: iso-latin-1 code
 * This allows for the whole unicode range to be encoded, including chars outside of
 * the BMP. Also everything in the iso-latin-1 charset is only 1 byte, except control
 * characters which should never happen anyway (and still work, but take 3 bytes).
 *
 * cached suggestions list is, ordered by frequency, at most PTNODE_MAX_CACHED_SUGGESTIONS:
 * <flags>     = | hasNext = 1 bit, 1 = yes, 0 = no : FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
 *               | reserved = 3 bits, must be 0
 *               | 4 bits : frequency : mask with FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY
 * <suggestions>=| string of characters at the char format described above, with the terminator
 *               | used to signal the end of the string.
 * if (FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT) goto bigram_and_shortcut_address_list_is
 *
 * shortcut string list is:
 * <byte size> = PTNODE_SHORTCUT_LIST_SIZE_SIZE bytes, big-endian: size of the list, in bytes.
 * <flags>     = | hasNext = 1 bit, 1 = yes, 0 = no : FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
 *               | reserved = 3 bits, must be 0
 *               | 4 bits : frequency : mask with FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY
 * <shortcut>  = | string of characters at the char format described above, with the terminator
 *               | used to signal the end of the string.
 * if (FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT goto flags
 */
public class BinaryDictionary {
    private static final int MAGIC_NUMBER = 0x9BC13AFE;
    private static final int VERSION100 = 100;

    static final int HEADER_SIZE = 12;

    // These flags are used only in the static dictionary.
    static final int MASK_CHILDREN_ADDRESS_TYPE = 0xC0;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_NOADDRESS = 0x00;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_ONEBYTE = 0x40;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_TWOBYTES = 0x80;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_THREEBYTES = 0xC0;

    static final int FLAG_HAS_MULTIPLE_CHARS = 0x20;

    static final int FLAG_IS_TERMINAL = 0x10;
    static final int FLAG_HAS_SHORTCUT_TARGETS = 0x08;
    static final int FLAG_HAS_BIGRAMS = 0x04;
    static final int FLAG_IS_NOT_A_WORD = 0x02;
    static final int FLAG_HAS_CACHED_SUGGESTIONS = 0x01;

    static final int FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT = 0x80;
    static final int FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY = 0x0F;

    static final int PTNODE_CHARACTERS_TERMINATOR = 0x1F;

    static final int PTNODE_TERMINATOR_SIZE = 1;
    static final int PTNODE_FLAGS_SIZE = 1;
    static final int PTNODE_FREQUENCY_SIZE = 1;
    static final int PTNODE_MAX_ADDRESS_SIZE = 3;
    static final int PTNODE_ATTRIBUTE_FLAGS_SIZE = 1;
    static final int PTNODE_ATTRIBUTE_MAX_ADDRESS_SIZE = 3;
    static final int PTNODE_SHORTCUT_LIST_SIZE_SIZE = 2;
    static final int PTNODE_MAX_CACHED_SUGGESTIONS = 10;

    static final int NO_CHILDREN_ADDRESS = Integer.MIN_VALUE;
    static final int INVALID_CHARACTER = -1;

    static final int MAX_PTNODES_FOR_ONE_BYTE_PTNODE_COUNT = 0x7F; // 127
    // Large PtNode array size field size is 2 bytes.
    static final int LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE_FLAG = 0x8000;
    static final int MAX_PTNODES_IN_A_PT_NODE_ARRAY = 0x7FFF; // 32767
    static final int MAX_BIGRAMS_IN_A_PTNODE = 10000;
    static final int MAX_SHORTCUT_LIST_SIZE_IN_A_PTNODE = 0xFFFF;

    static final int MAX_TERMINAL_FREQUENCY = 255;
    static final int MAX_BIGRAM_FREQUENCY = 15;

    public static final int SHORTCUT_WHITELIST_FREQUENCY = 15;

    static final int MINIMAL_ONE_BYTE_CHARACTER_VALUE = 0x20;
    static final int MAXIMAL_ONE_BYTE_CHARACTER_VALUE = 0xFF;

    /**
     * A string with a probability.
     *
     * This represents an "attribute", that is either a bigram or a shortcut.
     */
    public static final class WeightedString {
        public final String mWord;
        public int mFrequency;

        public WeightedString(final String word, final int probability) {
            mWord = word;
            mFrequency = probability;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] { mWord, mFrequency});
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof WeightedString)) return false;
            final WeightedString w = (WeightedString)o;
            return mWord.equals(w.mWord) && mFrequency == w.mFrequency;
        }
    }

    public static class DictionaryInvalidFormatException extends Exception {
        public DictionaryInvalidFormatException() {
            super();
        }

        public DictionaryInvalidFormatException(String message) {
            super(message);
        }
    }

    public static class QueryResults {
        public String word;
        public boolean valid;
        public int frequency;
        public List<WeightedString> suggestions;

        public QueryResults(String w, boolean v, int f, List<WeightedString> s) {
            word = w;
            valid = v;
            frequency = f;
            suggestions = s;
        }
    }

    private static class PtNode {
        public int pos;
        String preChars;
        public byte flag;
        public int[] chars;
        public int frequency;
        public int childrenPosition;
        public int readAfterPosition;
        public List<WeightedString> shortcuts;
        public List<WeightedString> cachedSuggestions;

        public static PtNode readPtNode(final byte[] buffer, final int position) throws DictionaryInvalidFormatException {
            int pos = position;
            PtNode res = new PtNode();
            res.pos = pos;
            res.flag = buffer[pos++];

            List<Integer> cs = new ArrayList<>();
            int[] character = readChar(buffer, pos);
            pos = character[1];
            if (res.hasMultipleChar()) {
                while (character[0] != INVALID_CHARACTER) {
                    cs.add(character[0]);
                    character = readChar(buffer, pos);
                    pos = character[1];
                }
            } else {
                cs.add(character[0]);
            }
            res.chars = new int[cs.size()];
            for (int i = 0; i < cs.size(); i++) {
                res.chars[i] = cs.get(i);
            }

            if (res.isTerminal()) {
                res.frequency = readUnsignedByte(buffer, pos);
                pos++;
            }
            int addressSize = getChildrenAddressSize(res.flag);
            res.childrenPosition = getChildrenAddress(buffer, pos, addressSize);
            pos += addressSize;

            if (res.hasCachedSuggestions()) {
                int cachedSize = readUnsignedShort(buffer, pos);
                pos += PTNODE_SHORTCUT_LIST_SIZE_SIZE;
                res.cachedSuggestions = new ArrayList<>();
                pos = readWeightedStrings(buffer, pos, res.cachedSuggestions);
                if (cachedSize != res.cachedSuggestions.size()) {
                    throw new DictionaryInvalidFormatException("cached suggestion size didn't match, binary size = " + cachedSize + ", parsed size = " + res.cachedSuggestions.size());
                }
            }

            if (res.isTerminal() && res.hasShortcuts()) {
                int shortcutSize = readUnsignedShort(buffer, pos);
                pos += PTNODE_SHORTCUT_LIST_SIZE_SIZE;
                res.shortcuts = new ArrayList<>();
                pos = readWeightedStrings(buffer, pos, res.shortcuts);
                if (shortcutSize != res.shortcuts.size()) {
                    throw new DictionaryInvalidFormatException("shortcut size didn't match");
                }
            }

            res.readAfterPosition = pos;
            return res;
        }

        private static int readWeightedStrings(byte[] buffer, int position, List<WeightedString> res) {
            int pos = position;
            boolean hasNext = true;
            while (hasNext) {
                byte flag = buffer[pos++];
                hasNext = (flag & FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT) != 0;
                int f = (flag & FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY);
                StringBuilder sb = new StringBuilder();
                pos = readString(buffer, pos, sb);
                res.add(new WeightedString(sb.toString(), f));
            }
            return pos;
        }

        public boolean isTerminal() {
            return (flag & FLAG_IS_TERMINAL) != 0;
        }

        public boolean isWord() {
            return (flag & FLAG_IS_NOT_A_WORD) == 0;
        }

        public boolean hasCachedSuggestions() {
            return (flag & FLAG_HAS_CACHED_SUGGESTIONS) != 0;
        }

        public boolean hasShortcuts() {
            return (flag & FLAG_HAS_SHORTCUT_TARGETS) != 0;
        }

        private boolean hasMultipleChar() {
            return (flag & FLAG_HAS_MULTIPLE_CHARS) != 0;
        }
    }

    private static class FuseSearch {
        static final int EDITED = 0x0F;

        static final int DELETED = 0x01;
        static final int INSERTED = 0x02;
        static final int REPLACED = 0x04;
        static final int INTER = 0x08;
        static final int INTERED = 0x10;

        StringBuilder prefix;
        int nodePosition;
        int codePosition;
        int editMode;

        public FuseSearch(StringBuilder sb, int bp, int cp, int e) {
            prefix = sb;
            nodePosition = bp;
            codePosition = cp;
            editMode = e;
        }
    }

    private byte[] data;

    public BinaryDictionary(File dictionary) throws IOException, DictionaryInvalidFormatException {
        data = Files.readAllBytes(dictionary.toPath());
        parseHeader(data);
    }

    private static void parseHeader(byte[] buffer) throws DictionaryInvalidFormatException {
        if (buffer[0] != (byte)(0xFF & (MAGIC_NUMBER >> 24)) ||
                buffer[1] != (byte)(0xFF & (MAGIC_NUMBER >> 16)) ||
                buffer[2] != (byte)(0xFF & (MAGIC_NUMBER >> 8)) ||
                buffer[3] != (byte)(0xFF & MAGIC_NUMBER)) {
            throw new DictionaryInvalidFormatException("invalid magic header");
        }
        int version = readUnsignedShort(buffer, 4);
        if (version != VERSION100) {
            throw new DictionaryInvalidFormatException("unsupported dictionary version");
        }
        Log.i("[DICT]", "dictionary loaded, version = " + version);
    }

    // run in background thread
    private QueryResults query(String word) throws DictionaryInvalidFormatException {
        StringBuilder sb = new StringBuilder();
        PtNode node = searchPtNode(sb, data, HEADER_SIZE, getCodePoints(word), 0);
        if (node == null) {
            return null;
        }
        StringBuilder current = new StringBuilder(sb);
        for (int c : node.chars) {
            current.appendCodePoint(c);
        }
        boolean exactlyMatch = word.equals(current.toString());
        List<WeightedString> words = new ArrayList<>();
        if (node.hasCachedSuggestions()) {
            words = node.cachedSuggestions;
        } else {
            collectWords(sb, node, words);
            words.sort(Comparator.comparingInt(e -> MAX_TERMINAL_FREQUENCY - e.mFrequency));
            if (exactlyMatch && node.hasShortcuts() && node.shortcuts != null && !node.shortcuts.isEmpty()) {
                words.addAll(0, node.shortcuts);
            }
        }
        List<WeightedString> suggestions = words.stream().limit(PTNODE_MAX_CACHED_SUGGESTIONS).collect(Collectors.toList());
        boolean valid = exactlyMatch && node.isTerminal() && node.isWord();
        return new QueryResults(word, valid, valid ? node.frequency : 0, suggestions);
    }

    public QueryResults fuseQuery(String word) throws DictionaryInvalidFormatException {
        if (TextUtils.isEmpty(word)) {
            return null;
        }
        QueryResults res = query(word);
        if (res != null && res.suggestions.size() >= 3) {
            return res;
        }

        int[] upperCodes = getCodePoints(word);
        upperCodes[0] = Character.toUpperCase(upperCodes[0]);
        QueryResults upperMatch = query(new String(upperCodes, 0, upperCodes.length));
        if (upperMatch != null && upperMatch.suggestions != null && !upperMatch.suggestions.isEmpty()) {
            if (res == null) {
                return upperMatch;
            } else {
                res.suggestions.addAll(upperMatch.suggestions);
                res.suggestions.sort(Comparator.comparingInt(e -> MAX_TERMINAL_FREQUENCY - e.mFrequency));
                return res;
            }
        }

        // edit 1|2 distance match
        List<PtNode> node = fuseSearchPtNode(data, getCodePoints(word), new FuseSearch(new StringBuilder(), HEADER_SIZE, 0, 0));
        if (node == null || node.isEmpty()) {
            return res;
        }
        List<WeightedString> suggests = node.stream().flatMap(n -> {
            List<WeightedString> can = new ArrayList<>();
            try {
                collectWords(new StringBuilder(n.preChars), n, can);
            } catch (DictionaryInvalidFormatException e) {
                throw new RuntimeException(e.getMessage());
            }
            return can.stream();
        }).sorted(Comparator.comparingInt(e -> MAX_TERMINAL_FREQUENCY - e.mFrequency))
                .limit(PTNODE_MAX_CACHED_SUGGESTIONS).collect(Collectors.toList());
        if (res == null) {
            return new QueryResults(word, false, 0, suggests);
        } else {
            res.suggestions.addAll(suggests);
            return res;
        }
    }

    // this method will not collect cached suggestions, it should never be a case.
    private void collectWords(StringBuilder sb, PtNode node, List<WeightedString> words) throws DictionaryInvalidFormatException {
        for (int c : node.chars) {
            sb.appendCodePoint(c);
        }
        if (node.isTerminal()) {
            if (node.isWord()) {
                words.add(new WeightedString(sb.toString(), node.frequency));
            } else if (node.hasShortcuts() && !node.shortcuts.isEmpty()) {
                // make shortcuts higher priority
                node.shortcuts.forEach(e -> e.mFrequency = Math.min(MAX_TERMINAL_FREQUENCY, (node.frequency + SHORTCUT_WHITELIST_FREQUENCY - e.mFrequency)));
                words.addAll(node.shortcuts);
            }
        }
        if (node.childrenPosition > 0) {
            int position = node.childrenPosition;
            int[] nc = readPtNodeCount(data, position);
            int count = nc[0], pos = nc[1];
            for (int i = 0; i < count; i++) {
                PtNode n = PtNode.readPtNode(data, pos);
                pos = n.readAfterPosition;
                collectWords(new StringBuilder(sb), n, words);
            }
        }
    }

    /**
     * Helper method to convert a String to an int array.
     */
    private static int[] getCodePoints(final String word) {
        // TODO: this is a copy-paste of the old contents of StringUtils.toCodePointArray,
        // which is not visible from the makedict package. Factor this code.
        final int length = word.length();
        if (length <= 0) return new int[] {};
        final char[] characters = word.toCharArray();
        final int[] codePoints = new int[Character.codePointCount(characters, 0, length)];
        int codePoint = Character.codePointAt(characters, 0);
        int dsti = 0;
        for (int srci = Character.charCount(codePoint);
             srci < length; srci += Character.charCount(codePoint), ++dsti) {
            codePoints[dsti] = codePoint;
            codePoint = Character.codePointAt(characters, srci);
        }
        codePoints[dsti] = codePoint;
        return codePoints;
    }

    private static List<PtNode> fuseSearchPtNode(byte[] buffer, int[] codes, FuseSearch search) throws DictionaryInvalidFormatException {
        List<PtNode> res = new ArrayList<>();

        int[] nc = readPtNodeCount(buffer, search.nodePosition);
        int count = nc[0], pos = nc[1];
        for (int i = 0; i < count; i++) {
            PtNode node = PtNode.readPtNode(buffer, pos);
            node.preChars = search.prefix.toString();

            LinkedList<int[]> queue = new LinkedList<>();
            queue.offer(new int[]{0, search.codePosition, search.editMode});
            while (!queue.isEmpty()) {
                int[] tmp = queue.poll();
                int ni = tmp[0], cp = tmp[1], mode = tmp[2];
                while (ni < node.chars.length && cp < codes.length && codes[cp] == node.chars[ni]) {
                    cp++;
                    ni++;
                }

                // codes different
                if (ni < node.chars.length && cp < codes.length) {
                    if (mode != 0 && mode != FuseSearch.INTER) {
                        continue;
                    }
                    if (mode == FuseSearch.INTER) {
                        if (cp > 0 && node.chars[ni] == codes[cp-1]) {
                            queue.offer(new int[]{ni+1, cp+1, FuseSearch.INTERED});
                        }
                        continue;
                    }
                    if (cp+1 == codes.length) {
                        res.add(node);
                        continue;
                    }
                    // cp+1 < codes.len
                    queue.offer(new int[]{ni+1, cp+1, FuseSearch.REPLACED}); // replace
                    queue.offer(new int[]{ni, cp+1, FuseSearch.DELETED}); // delete
                    queue.offer(new int[]{ni+1, cp, FuseSearch.INSERTED}); // inserted
                    if (codes[cp+1] == node.chars[ni]) {
                        queue.offer(new int[]{ni+1, cp+1, FuseSearch.INTER}); // interpolate
                    }
                    continue;
                }

                // codes same, cp == codes.length
                if (cp == codes.length) {
                    res.add(node);
                    break;
                }

                // cp < codes.len, search children
                assert ni == node.chars.length;
                if (node.childrenPosition <= 0) {
                    continue;
                }
                StringBuilder sb = new StringBuilder(search.prefix);
                for (int t = 0; t < node.chars.length; t++) {
                    sb.appendCodePoint(node.chars[t]);
                }
                res.addAll(fuseSearchPtNode(buffer, codes, new FuseSearch(sb, node.childrenPosition, cp, mode)));
            }

            pos = node.readAfterPosition;
        }
        return res;
    }

    private static PtNode searchPtNode(StringBuilder sb, byte[] buffer, int bp, int[] codes, int cp)
            throws DictionaryInvalidFormatException {
        int[] nc = readPtNodeCount(buffer, bp);
        int count = nc[0], pos = nc[1];
        for (int i = 0; i < count; i++) {
            PtNode node = PtNode.readPtNode(buffer, pos);
            if (node.chars[0] != codes[cp]) {
                pos = node.readAfterPosition;
                continue;
            }
            // go into this node
            int ni = 0;
            while (ni < node.chars.length && cp < codes.length && codes[cp] == node.chars[ni]) {
                cp++;
                ni++;
            }
            // codes different
            if (ni < node.chars.length && cp < codes.length) {
                return null;
            }
            // codes same, cp == codes.length
            if (cp == codes.length) {
                return node;
            }
            // cp < codes.len, search children
            assert ni == node.chars.length;
            if (node.childrenPosition <= 0) {
                return null;
            }
            for (int t = 0; t < node.chars.length; t++) {
                sb.appendCodePoint(node.chars[t]);
            }
            return searchPtNode(sb, buffer, node.childrenPosition, codes, cp);
        }
        return null;
    }

    /**
     * Reads and returns the PtNode count out of a buffer and forwards the pointer.
     */
    private static int[] readPtNodeCount(final byte[] buffer, int position) {
        int msb = readUnsignedByte(buffer, position);
        if (MAX_PTNODES_FOR_ONE_BYTE_PTNODE_COUNT >= msb) {
            return new int[]{msb, position+1};
        }
        msb = ((MAX_PTNODES_FOR_ONE_BYTE_PTNODE_COUNT & msb) << 8)
                + readUnsignedByte(buffer, position+1);
        return new int[]{msb, position+2};
    }

    /**
     * Reads a string from a DictBuffer. This is the converse of the above method.
     */
    private static int readString(final byte[] buffer, int position, StringBuilder sb) {
        int[] character = readChar(buffer, position);
        int pos = character[1];
        while (character[0] != INVALID_CHARACTER) {
            sb.appendCodePoint(character[0]);
            character = readChar(buffer, pos);
            pos = character[1];
        }
        return pos;
    }

    /**
     * Reads a character from the buffer.
     *
     * This follows the character format documented earlier in this source file.
     *
     * @return the character code.
     */
    private static int[] readChar(final byte[] buffer, final int position) {
        int pos = position;
        int character = readUnsignedByte(buffer, pos);
        if (!fitsOnOneByte(character)) {
            if (PTNODE_CHARACTERS_TERMINATOR == character) {
                return new int[]{INVALID_CHARACTER, pos+1};
            }
            character <<= 16;
            character += readUnsignedShort(buffer, position+1);
            pos += 3;
        } else {
            pos += 1;
        }
        return new int[]{character, pos};
    }

    /**
     * Helper method to find out whether this code fits on one byte
     */
    private static boolean fitsOnOneByte(final int character) {
        int codePoint = character;
        return codePoint >= MINIMAL_ONE_BYTE_CHARACTER_VALUE
                && codePoint <= MAXIMAL_ONE_BYTE_CHARACTER_VALUE;
    }

    private static int readUnsignedByte(byte[] buffer, int position) {
        return buffer[position] & 0xFF;
    }

    private static int readUnsignedShort(byte[] buffer, int position) {
        final int retval = readUnsignedByte(buffer, position);
        return (retval << 8) + readUnsignedByte(buffer, position+1);
    }

    private static int readUnsignedInt24(byte[] buffer, int position) {
        final int retval = readUnsignedShort(buffer, position);
        return (retval << 8) + readUnsignedByte(buffer, position+2);
    }

    private static int readInt(byte[] buffer, int position) {
        final int retval = readUnsignedShort(buffer, position);
        return (retval << 16) + readUnsignedShort(buffer, position+2);
    }

    private static int getChildrenAddress(byte[] buffer, int position, int size) {
        if (size == 0) {
            return 0;
        } else if (size == 1) {
            return readUnsignedByte(buffer, position) + position;
        } else if (size == 2) {
            return position + readUnsignedShort(buffer, position);
        } else if (size == 3) {
            return position + readUnsignedInt24(buffer, position);
        } else {
            throw new RuntimeException("invalid children address");
        }
    }

    private static int getChildrenAddressSize(final int optionFlags) {
        switch (optionFlags & MASK_CHILDREN_ADDRESS_TYPE) {
            case FLAG_CHILDREN_ADDRESS_TYPE_ONEBYTE:
                return 1;
            case FLAG_CHILDREN_ADDRESS_TYPE_TWOBYTES:
                return 2;
            case FLAG_CHILDREN_ADDRESS_TYPE_THREEBYTES:
                return 3;
            case FLAG_CHILDREN_ADDRESS_TYPE_NOADDRESS:
            default:
                return 0;
        }
    }
}
