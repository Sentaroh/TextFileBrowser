package com.sentaroh.android.TextFileBrowser;
/*
The MIT License (MIT)
Copyright (c) 2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

class Constants {
	public static final String APPLICATION_TAG="TextFileBrowser";
    public static final String APPLICATION_ID="com.sentaroh.android."+APPLICATION_TAG;
    public static final String DEFAULT_LINE_BREAK=String.valueOf(CustomTextView.LINE_BREAK_NO_WORD_WRAP);
	public static final String DEFAULT_FONT_FAMILY="MONOSPACE";
	public static final String DEFAULT_FONT_STYLE="NORMAL";
	public static final String DEFAULT_FONT_SIZE="15";
	public static final String DEFAULT_TAB_STOP="4";

	public static final String ENCODE_NAME_UTF8="UTF-8";

    public static final String TEXT_AREA_BACKGROUND_COLOR_LIGHT ="#b3ffffff";
    public static final String TEXT_AREA_BACKGROUND_COLOR_DARK="#ff404040";

    public static final String TEXT_MODE_MIME_TYPE="text/*;audio/x-mpegurl;application/x-mpegurl;application/vnd.apple.mpegurl;application/octet-stream";

	public static final String INDEX_CACHE_NO_CACHE="0",
			INDEX_CACHE_UP_TO_10MB="1",
			INDEX_CACHE_UP_TO_50MB="2",
			INDEX_CACHE_NO_LIMIT="3";
}
