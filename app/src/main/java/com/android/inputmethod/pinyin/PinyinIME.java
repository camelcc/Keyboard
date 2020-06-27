/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.pinyin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Main class of the Pinyin input method.
 */
public class PinyinIME {
    public static boolean sPrediction = true;

    public static interface PinyinIMEListener {
        void commitText(String text);
        void commitCompletion(CompletionInfo ci);
        CharSequence getTextBeforeCursor(int length);
    }

    private static final String TAG = "PinyinIME";

    /**
     * Connection used to bind the decoding service.
     */
    private PinyinDecoderServiceConnection mPinyinDecoderServiceConnection;

    /**
     * The current IME status.
     *
     * @see com.android.inputmethod.pinyin.PinyinIME.ImeState
     */
    private ImeState mImeState = ImeState.STATE_IDLE;

    /**
     * The decoding information, include spelling(Pinyin) string, decoding
     * result, etc.
     */
    private DecodingInfo mDecInfo = new DecodingInfo();

    private PinyinIMEListener mListener = null;

    private Context mContext;

    public PinyinIME(Context context) {
        mContext = context;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate.");
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(mContext, PinyinDecoderService.class);
        if (null == mPinyinDecoderServiceConnection) {
            mPinyinDecoderServiceConnection = new PinyinDecoderServiceConnection();
        }
        // Bind service
        mContext.bindService(serviceIntent, mPinyinDecoderServiceConnection,
            Context.BIND_AUTO_CREATE);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy.");
        mContext.unbindService(mPinyinDecoderServiceConnection);
    }

    public void reset() {
        mImeState = ImeState.STATE_IDLE;
        mDecInfo.reset();
    }

    public void setListener(PinyinIMEListener listener) {
        mListener = listener;
    }

    public void processText(char keyChar) {
        if (keyChar >= 'a' && keyChar <= 'z') {
            mDecInfo.addSplChar((char) keyChar, mImeState == ImeState.STATE_IDLE || mImeState == ImeState.STATE_PREDICT);
            if (mImeState == ImeState.STATE_PREDICT) {
                mImeState = ImeState.STATE_INPUT;
            }
            chooseAndUpdate(-1);
            return;
        }
        if (mImeState == ImeState.STATE_INPUT && keyChar == '\'' && !mDecInfo.charBeforeCursorIsSeparator()) {
            mDecInfo.addSplChar((char) keyChar, false);
            chooseAndUpdate(-1);
            return;
        }
        if (mListener != null) {
            mListener.commitText((mImeState == ImeState.STATE_INPUT || mImeState == ImeState.STATE_COMPOSING) ?
                mDecInfo.getCurrentFullSent(0) + keyChar : String.valueOf(keyChar));
        }
        mDecInfo.reset();
        mImeState = ImeState.STATE_IDLE;
    }

    public boolean processKeycode(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mImeState == ImeState.STATE_INPUT || mImeState == ImeState.STATE_COMPOSING) {
                mDecInfo.prepareDeleteBeforeCursor();
                chooseAndUpdate(-1);
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mImeState == ImeState.STATE_INPUT || mImeState == ImeState.STATE_COMPOSING) {
                if (mListener != null) {
                    mListener.commitText(mDecInfo.getComposingStr());
                }
                mDecInfo.reset();
                mImeState = ImeState.STATE_IDLE;
                return true;
            } else {
                mDecInfo.reset();
                mImeState = ImeState.STATE_IDLE;
            }
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            if (mImeState == ImeState.STATE_INPUT || mImeState == ImeState.STATE_COMPOSING) {
                chooseAndUpdate(0);
            } else {
                if (mListener != null) {
                    mListener.commitText(" ");
                }
                mDecInfo.reset();
                mImeState = ImeState.STATE_IDLE;
            }
            return true;
        }
        return false;
    }

    private void chooseAndUpdate(int candId) {
        if (ImeState.STATE_PREDICT != mImeState) {
            // Get result candidate list, if choice_id < 0, do a new decoding.
            // If choice_id >=0, select the candidate, and get the new candidate
            // list.
            mDecInfo.chooseDecodingCandidate(candId);
        } else {
            // Choose a prediction item.
            mDecInfo.choosePredictChoice(candId);
        }

        if (mDecInfo.getComposingStr().length() > 0) {
            String resultStr;
            resultStr = mDecInfo.getComposingStrActivePart();

            // choiceId >= 0 means user finishes a choice selection.
            if (candId >= 0 && mDecInfo.canDoPrediction()) {
                if (mListener != null) {
                    mListener.commitText(resultStr);
                }
                mImeState = ImeState.STATE_PREDICT;
                // Try to get the prediction list.
                if (sPrediction && mListener != null) {
                    CharSequence cs = mListener.getTextBeforeCursor(3);
                    if (cs != null) {
                        mDecInfo.preparePredicts(cs);
                    }
                } else {
                    mDecInfo.resetCandidates();
                }

                if (mDecInfo.mCandidatesList.isEmpty()) {
                    mDecInfo.reset();
                    mImeState = ImeState.STATE_IDLE;
                }
            } else {
                if (ImeState.STATE_IDLE == mImeState) {
                    if (mDecInfo.getSplStrDecodedLen() == 0) {
                        mImeState = ImeState.STATE_COMPOSING;
                    } else {
                        mImeState = ImeState.STATE_INPUT;
                    }
                } else {
                    if (mDecInfo.selectionFinished()) {
                        mImeState = ImeState.STATE_COMPOSING;
                    }
                }
            }
        } else {
            mDecInfo.reset();
            mImeState = ImeState.STATE_IDLE;
        }
    }

    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (null == completions || completions.length <= 0) return;

        if (ImeState.STATE_IDLE == mImeState ||
            ImeState.STATE_PREDICT == mImeState) {
            mImeState = ImeState.STATE_APP_COMPLETION;
            mDecInfo.prepareAppCompletions(completions);
        }
    }

    public void onChoiceTouched(int activeCandNo) {
        if (mImeState == ImeState.STATE_COMPOSING) {
            mImeState = ImeState.STATE_INPUT;
        } else if (mImeState == ImeState.STATE_INPUT
                || mImeState == ImeState.STATE_PREDICT) {
            chooseAndUpdate(Math.max(activeCandNo, 0));
        } else if (mImeState == ImeState.STATE_APP_COMPLETION) {
            if (null != mDecInfo.mAppCompletions && activeCandNo >= 0 &&
                    activeCandNo < mDecInfo.mAppCompletions.length) {
                CompletionInfo ci = mDecInfo.mAppCompletions[activeCandNo];
                if (null != ci && mListener != null) {
                    mListener.commitCompletion(ci);
                }
            }
            mDecInfo.reset();
            mImeState = ImeState.STATE_IDLE;
        }
    }

    public DecodingInfo getDecInfo() {
        return mDecInfo;
    }

    public List<String> getCandidates() {
        mDecInfo.preparePage(0);
        return mDecInfo.mCandidatesList;
    }

    public String getDisplayComposing() {
        if (mImeState == ImeState.STATE_INPUT || mImeState == ImeState.STATE_COMPOSING) {
            return mDecInfo.mComposingStrDisplay;
        } else {
            return "";
        }
    }

    /**
     * Connection used for binding to the Pinyin decoding service.
     */
    public class PinyinDecoderServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDecInfo.mIPinyinDecoderService = IPinyinDecoderService.Stub
                    .asInterface(service);
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    public enum ImeState {
        STATE_IDLE, STATE_INPUT, STATE_COMPOSING, STATE_PREDICT,
        STATE_APP_COMPLETION
    }

    public class DecodingInfo {
        /**
         * Maximum length of the Pinyin string
         */
        private static final int PY_STRING_MAX = 28;

        /**
         * Maximum number of candidates to display in one page.
         */
        private static final int MAX_PAGE_SIZE_DISPLAY = 100;

        /**
         * Spelling (Pinyin) string.
         */
        private StringBuffer mSurface;

        /**
         * Byte buffer used as the Pinyin string parameter for native function
         * call.
         */
        private byte mPyBuf[];

        /**
         * The length of surface string successfully decoded by engine.
         */
        private int mSurfaceDecodedLen;

        /**
         * Composing string.
         */
        private String mComposingStr;

        /**
         * Length of the active composing string.
         */
        private int mActiveCmpsLen;

        /**
         * Composing string for display, it is copied from mComposingStr, and
         * add spaces between spellings.
         **/
        private String mComposingStrDisplay;

        /**
         * Length of the active composing string for display.
         */
        private int mActiveCmpsDisplayLen;

        /**
         * The first full sentence choice.
         */
        private String mFullSent;

        /**
         * Number of characters which have been fixed.
         */
        private int mFixedLen;

        /**
         * If this flag is true, selection is finished.
         */
        private boolean mFinishSelection;

        /**
         * The starting position for each spelling. The first one is the number
         * of the real starting position elements.
         */
        private int mSplStart[];

        /**
         * Editing cursor in mSurface.
         */
        private int mCursorPos;

        /**
         * Remote Pinyin-to-Hanzi decoding engine service.
         */
        private IPinyinDecoderService mIPinyinDecoderService;

        /**
         * The complication information suggested by application.
         */
        private CompletionInfo[] mAppCompletions;

        /**
         * The total number of choices for display. The list may only contains
         * the first part. If user tries to navigate to next page which is not
         * in the result list, we need to get these items.
         **/
        public int mTotalChoicesNum;

        /**
         * Candidate list. The first one is the full-sentence candidate.
         */
        public List<String> mCandidatesList = new Vector<String>();

        /**
         * Element i stores the starting position of page i.
         */
        public Vector<Integer> mPageStart = new Vector<Integer>();

        /**
         * Element i stores the number of characters to page i.
         */
        public Vector<Integer> mCnToPage = new Vector<Integer>();

        /**
         * The position to delete in Pinyin string. If it is less than 0, IME
         * will do an incremental search, otherwise IME will do a deletion
         * operation. if {@link #mIsPosInSpl} is true, IME will delete the whole
         * string for mPosDelSpl-th spelling, otherwise it will only delete
         * mPosDelSpl-th character in the Pinyin string.
         */
        public int mPosDelSpl = -1;

        /**
         * If {@link #mPosDelSpl} is big than or equal to 0, this member is used
         * to indicate that whether the postion is counted in spelling id or
         * character.
         */
        public boolean mIsPosInSpl;

        public DecodingInfo() {
            mSurface = new StringBuffer();
            mSurfaceDecodedLen = 0;
        }

        public void reset() {
            mSurface.delete(0, mSurface.length());
            mSurfaceDecodedLen = 0;
            mCursorPos = 0;
            mFullSent = "";
            mFixedLen = 0;
            mFinishSelection = false;
            mComposingStr = "";
            mComposingStrDisplay = "";
            mActiveCmpsLen = 0;
            mActiveCmpsDisplayLen = 0;

            resetCandidates();
        }

        public boolean isCandidatesListEmpty() {
            return mCandidatesList.size() == 0;
        }

        public boolean isSplStrFull() {
            if (mSurface.length() >= PY_STRING_MAX - 1) return true;
            return false;
        }

        public void addSplChar(char ch, boolean reset) {
            if (reset) {
                mSurface.delete(0, mSurface.length());
                mSurfaceDecodedLen = 0;
                mCursorPos = 0;
                try {
                    mIPinyinDecoderService.imResetSearch();
                } catch (RemoteException e) {
                }
            }
            mSurface.insert(mCursorPos, ch);
            mCursorPos++;
        }

        // Prepare to delete before cursor. We may delete a spelling char if
        // the cursor is in the range of unfixed part, delete a whole spelling
        // if the cursor in inside the range of the fixed part.
        // This function only marks the position used to delete.
        public void prepareDeleteBeforeCursor() {
            if (mCursorPos > 0) {
                int pos;
                for (pos = 0; pos < mFixedLen; pos++) {
                    if (mSplStart[pos + 2] >= mCursorPos
                            && mSplStart[pos + 1] < mCursorPos) {
                        mPosDelSpl = pos;
                        mCursorPos = mSplStart[pos + 1];
                        mIsPosInSpl = true;
                        break;
                    }
                }
                if (mPosDelSpl < 0) {
                    mPosDelSpl = mCursorPos - 1;
                    mCursorPos--;
                    mIsPosInSpl = false;
                }
            }
        }

        public int length() {
            return mSurface.length();
        }

        public char charAt(int index) {
            return mSurface.charAt(index);
        }

        public StringBuffer getOrigianlSplStr() {
            return mSurface;
        }

        public int getSplStrDecodedLen() {
            return mSurfaceDecodedLen;
        }

        public int[] getSplStart() {
            return mSplStart;
        }

        public String getComposingStr() {
            return mComposingStr;
        }

        public String getComposingStrActivePart() {
            assert (mActiveCmpsLen <= mComposingStr.length());
            return mComposingStr.substring(0, mActiveCmpsLen);
        }

        public int getActiveCmpsLen() {
            return mActiveCmpsLen;
        }

        public String getComposingStrForDisplay() {
            return mComposingStrDisplay;
        }

        public int getActiveCmpsDisplayLen() {
            return mActiveCmpsDisplayLen;
        }

        public String getFullSent() {
            return mFullSent;
        }

        public String getCurrentFullSent(int activeCandPos) {
            try {
                String retStr = mFullSent.substring(0, mFixedLen);
                retStr += mCandidatesList.get(activeCandPos);
                return retStr;
            } catch (Exception e) {
                return "";
            }
        }

        public void resetCandidates() {
            mCandidatesList.clear();
            mTotalChoicesNum = 0;

            mPageStart.clear();
            mPageStart.add(0);
            mCnToPage.clear();
            mCnToPage.add(0);
        }

        public boolean candidatesFromApp() {
            return ImeState.STATE_APP_COMPLETION == mImeState;
        }

        public boolean canDoPrediction() {
            return mComposingStr.length() == mFixedLen;
        }

        public boolean selectionFinished() {
            return mFinishSelection;
        }

        // After the user chooses a candidate, input method will do a
        // re-decoding and give the new candidate list.
        // If candidate id is less than 0, means user is inputting Pinyin,
        // not selecting any choice.
        private void chooseDecodingCandidate(int candId) {
            if (mImeState != ImeState.STATE_PREDICT) {
                resetCandidates();
                int totalChoicesNum = 0;
                try {
                    if (candId < 0) {
                        if (length() == 0) {
                            totalChoicesNum = 0;
                        } else {
                            if (mPyBuf == null)
                                mPyBuf = new byte[PY_STRING_MAX];
                            for (int i = 0; i < length(); i++)
                                mPyBuf[i] = (byte) charAt(i);
                            mPyBuf[length()] = 0;

                            if (mPosDelSpl < 0) {
                                totalChoicesNum = mIPinyinDecoderService
                                        .imSearch(mPyBuf, length());
                            } else {
                                boolean clear_fixed_this_step = true;
                                if (ImeState.STATE_COMPOSING == mImeState) {
                                    clear_fixed_this_step = false;
                                }
                                totalChoicesNum = mIPinyinDecoderService
                                        .imDelSearch(mPosDelSpl, mIsPosInSpl,
                                                clear_fixed_this_step);
                                mPosDelSpl = -1;
                            }
                        }
                    } else {
                        totalChoicesNum = mIPinyinDecoderService
                                .imChoose(candId);
                    }
                } catch (RemoteException e) {
                }
                updateDecInfoForSearch(totalChoicesNum);
            }
        }

        private void updateDecInfoForSearch(int totalChoicesNum) {
            mTotalChoicesNum = totalChoicesNum;
            if (mTotalChoicesNum < 0) {
                mTotalChoicesNum = 0;
                return;
            }

            try {
                String pyStr;

                mSplStart = mIPinyinDecoderService.imGetSplStart();
                pyStr = mIPinyinDecoderService.imGetPyStr(false);
                mSurfaceDecodedLen = mIPinyinDecoderService.imGetPyStrLen(true);
                assert (mSurfaceDecodedLen <= pyStr.length());

                mFullSent = mIPinyinDecoderService.imGetChoice(0);
                mFixedLen = mIPinyinDecoderService.imGetFixedLen();

                // Update the surface string to the one kept by engine.
                mSurface.replace(0, mSurface.length(), pyStr);

                if (mCursorPos > mSurface.length())
                    mCursorPos = mSurface.length();
                mComposingStr = mFullSent.substring(0, mFixedLen)
                        + mSurface.substring(mSplStart[mFixedLen + 1]);

                mActiveCmpsLen = mComposingStr.length();
                if (mSurfaceDecodedLen > 0) {
                    mActiveCmpsLen = mActiveCmpsLen
                            - (mSurface.length() - mSurfaceDecodedLen);
                }

                // Prepare the display string.
                if (0 == mSurfaceDecodedLen) {
                    mComposingStrDisplay = mComposingStr;
                    mActiveCmpsDisplayLen = mComposingStr.length();
                } else {
                    mComposingStrDisplay = mFullSent.substring(0, mFixedLen);
                    for (int pos = mFixedLen + 1; pos < mSplStart.length - 1; pos++) {
                        mComposingStrDisplay += mSurface.substring(
                                mSplStart[pos], mSplStart[pos + 1]);
                        if (mSplStart[pos + 1] < mSurfaceDecodedLen) {
                            mComposingStrDisplay += " ";
                        }
                    }
                    mActiveCmpsDisplayLen = mComposingStrDisplay.length();
                    if (mSurfaceDecodedLen < mSurface.length()) {
                        mComposingStrDisplay += mSurface
                                .substring(mSurfaceDecodedLen);
                    }
                }

                if (mSplStart.length == mFixedLen + 2) {
                    mFinishSelection = true;
                } else {
                    mFinishSelection = false;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "PinyinDecoderService died", e);
            } catch (Exception e) {
                mTotalChoicesNum = 0;
                mComposingStr = "";
            }
            // Prepare page 0.
            if (!mFinishSelection) {
                preparePage(0);
            }
        }

        private void choosePredictChoice(int choiceId) {
            if (ImeState.STATE_PREDICT != mImeState || choiceId < 0
                    || choiceId >= mTotalChoicesNum) {
                return;
            }

            String tmp = mCandidatesList.get(choiceId);

            resetCandidates();

            mCandidatesList.add(tmp);
            mTotalChoicesNum = 1;

            mSurface.replace(0, mSurface.length(), "");
            mCursorPos = 0;
            mFullSent = tmp;
            mFixedLen = tmp.length();
            mComposingStr = mFullSent;
            mActiveCmpsLen = mFixedLen;

            mFinishSelection = true;
        }

        public String getCandidate(int candId) {
            // Only loaded items can be gotten, so we use mCandidatesList.size()
            // instead mTotalChoiceNum.
            if (candId < 0 || candId > mCandidatesList.size()) {
                return null;
            }
            return mCandidatesList.get(candId);
        }

        public void getCandiagtesForCache() {
            Log.e("TTT", "pinyin ime load more item");
            int fetchStart = mCandidatesList.size();
            int fetchSize = mTotalChoicesNum - fetchStart;
            if (fetchSize > MAX_PAGE_SIZE_DISPLAY) {
                fetchSize = MAX_PAGE_SIZE_DISPLAY;
            }
            try {
                List<String> newList = null;
                if (ImeState.STATE_INPUT == mImeState ||
                        ImeState.STATE_IDLE == mImeState ||
                        ImeState.STATE_COMPOSING == mImeState){
                    newList = mIPinyinDecoderService.imGetChoiceList(
                            fetchStart, fetchSize, mFixedLen);
                } else if (ImeState.STATE_PREDICT == mImeState) {
                    newList = mIPinyinDecoderService.imGetPredictList(
                            fetchStart, fetchSize);
                } else if (ImeState.STATE_APP_COMPLETION == mImeState) {
                    newList = new ArrayList<String>();
                    if (null != mAppCompletions) {
                        for (int pos = fetchStart; pos < fetchSize; pos++) {
                            CompletionInfo ci = mAppCompletions[pos];
                            if (null != ci) {
                                CharSequence s = ci.getText();
                                if (null != s) newList.add(s.toString());
                            }
                        }
                    }
                }
                mCandidatesList.addAll(newList);
            } catch (RemoteException e) {
                Log.w(TAG, "PinyinDecoderService died", e);
            }
        }

        public boolean pageReady(int pageNo) {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false;

            // Page pageNo's ending information is not ready.
            if (mPageStart.size() <= pageNo + 1) {
                return false;
            }

            return true;
        }

        public boolean preparePage(int pageNo) {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false;

            // Make sure the starting information for page pageNo is ready.
            if (mPageStart.size() <= pageNo) {
                return false;
            }

            // Page pageNo's ending information is also ready.
            if (mPageStart.size() > pageNo + 1) {
                return true;
            }

            // If cached items is enough for page pageNo.
            if (mCandidatesList.size() - mPageStart.elementAt(pageNo) >= MAX_PAGE_SIZE_DISPLAY) {
                return true;
            }

            // Try to get more items from engine
            getCandiagtesForCache();

            // Try to find if there are available new items to display.
            // If no new item, return false;
            if (mPageStart.elementAt(pageNo) >= mCandidatesList.size()) {
                return false;
            }

            // If there are new items, return true;
            return true;
        }

        public void preparePredicts(CharSequence history) {
            if (null == history) return;

            resetCandidates();

            if (sPrediction) {
                String preEdit = history.toString();
                int predictNum = 0;
                if (null != preEdit) {
                    try {
                        mTotalChoicesNum = mIPinyinDecoderService
                                .imGetPredictsNum(preEdit);
                    } catch (RemoteException e) {
                        return;
                    }
                }
            }

            preparePage(0);
            mFinishSelection = false;
        }

        private void prepareAppCompletions(CompletionInfo completions[]) {
            resetCandidates();
            mAppCompletions = completions;
            mTotalChoicesNum = completions.length;
            preparePage(0);
            mFinishSelection = false;
        }

        public int getCurrentPageSize(int currentPage) {
            if (mPageStart.size() <= currentPage + 1) return 0;
            return mPageStart.elementAt(currentPage + 1)
                    - mPageStart.elementAt(currentPage);
        }

        public int getCurrentPageStart(int currentPage) {
            if (mPageStart.size() < currentPage + 1) return mTotalChoicesNum;
            return mPageStart.elementAt(currentPage);
        }

        public boolean pageForwardable(int currentPage) {
            if (mPageStart.size() <= currentPage + 1) return false;
            if (mPageStart.elementAt(currentPage + 1) >= mTotalChoicesNum) {
                return false;
            }
            return true;
        }

        public boolean pageBackwardable(int currentPage) {
            if (currentPage > 0) return true;
            return false;
        }

        public boolean charBeforeCursorIsSeparator() {
            int len = mSurface.length();
            if (mCursorPos > len) return false;
            if (mCursorPos > 0 && mSurface.charAt(mCursorPos - 1) == '\'') {
                return true;
            }
            return false;
        }

        public int getCursorPos() {
            return mCursorPos;
        }

        public int getCursorPosInCmps() {
            int cursorPos = mCursorPos;
            int fixedLen = 0;

            for (int hzPos = 0; hzPos < mFixedLen; hzPos++) {
                if (mCursorPos >= mSplStart[hzPos + 2]) {
                    cursorPos -= mSplStart[hzPos + 2] - mSplStart[hzPos + 1];
                    cursorPos += 1;
                }
            }
            return cursorPos;
        }

        public int getCursorPosInCmpsDisplay() {
            int cursorPos = getCursorPosInCmps();
            // +2 is because: one for mSplStart[0], which is used for other
            // purpose(The length of the segmentation string), and another
            // for the first spelling which does not need a space before it.
            for (int pos = mFixedLen + 2; pos < mSplStart.length - 1; pos++) {
                if (mCursorPos <= mSplStart[pos]) {
                    break;
                } else {
                    cursorPos++;
                }
            }
            return cursorPos;
        }

        public void moveCursorToEdge(boolean left) {
            if (left)
                mCursorPos = 0;
            else
                mCursorPos = mSurface.length();
        }

        // Move cursor. If offset is 0, this function can be used to adjust
        // the cursor into the bounds of the string.
        public void moveCursor(int offset) {
            if (offset > 1 || offset < -1) return;

            if (offset != 0) {
                int hzPos = 0;
                for (hzPos = 0; hzPos <= mFixedLen; hzPos++) {
                    if (mCursorPos == mSplStart[hzPos + 1]) {
                        if (offset < 0) {
                            if (hzPos > 0) {
                                offset = mSplStart[hzPos]
                                        - mSplStart[hzPos + 1];
                            }
                        } else {
                            if (hzPos < mFixedLen) {
                                offset = mSplStart[hzPos + 2]
                                        - mSplStart[hzPos + 1];
                            }
                        }
                        break;
                    }
                }
            }
            mCursorPos += offset;
            if (mCursorPos < 0) {
                mCursorPos = 0;
            } else if (mCursorPos > mSurface.length()) {
                mCursorPos = mSurface.length();
            }
        }

        public int getSplNum() {
            return mSplStart[0];
        }

        public int getFixedLen() {
            return mFixedLen;
        }
    }
}
