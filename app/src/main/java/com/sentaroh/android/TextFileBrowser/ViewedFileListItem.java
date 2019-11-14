package com.sentaroh.android.TextFileBrowser;

import android.net.Uri;

import com.sentaroh.android.Utilities2.SafFile3;
import com.sentaroh.android.Utilities2.ThreadCtrl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

class ViewedFileListItem implements Externalizable {
    public SafFile3 viewd_file=null;
    public String viewed_file_uri_string=null;
    public FileViewerFragment file_view_fragment=null;
    public ThreadCtrl tc_view=null;
    public IndexedFileReader ix_reader_view=null;

    public String encodeName="";

    public boolean viewerParmsInitRequired=true;
    public boolean viewerParmsRestoreRequired=false;

    public int[] listViewPos=new int[]{-1,-1};
    public int copyFrom=-1, copyTo=0;
    public int horizontalPos=0;
    public int findResultPos=-1;
    public boolean findPosIsValid=false;
    public boolean searchEnabled=false;
    public String searchString="";
    public boolean searchCaseSensitive=false;
    public boolean searchByWord=false;

    public int lineBreak=-1;
    public int browseMode=-1;
    public boolean showLineNo=true;

    public int adapterFindPosition=-1;
    public String adapterFindString="";


    ViewedFileListItem() {};

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        viewed_file_uri_string=in.readUTF();
        listViewPos[0]=in.readInt();
        listViewPos[1]=in.readInt();
        copyFrom=in.readInt();
        copyTo=in.readInt();
        horizontalPos=in.readInt();
        findResultPos=in.readInt();
        findPosIsValid=in.readBoolean();
        searchEnabled=in.readBoolean();
        searchString=in.readUTF();
        searchCaseSensitive=in.readBoolean();
        searchByWord=in.readBoolean();


        lineBreak=in.readInt();
        browseMode=in.readInt();
        showLineNo=in.readBoolean();

        encodeName=in.readUTF();
        viewerParmsInitRequired=in.readBoolean();
        viewerParmsRestoreRequired=in.readBoolean();

        adapterFindPosition=in.readInt();
        adapterFindString=in.readUTF();

    }
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(viewd_file.getUri().toString());
        out.writeInt(listViewPos[0]);
        out.writeInt(listViewPos[1]);
        out.writeInt(copyFrom);
        out.writeInt(copyTo);
        out.writeInt(horizontalPos);
        out.writeInt(findResultPos);
        out.writeBoolean(findPosIsValid);
        out.writeBoolean(searchEnabled);
        out.writeUTF(searchString);
        out.writeBoolean(searchCaseSensitive);
        out.writeBoolean(searchByWord);

        out.writeInt(lineBreak);
        out.writeInt(browseMode);
        out.writeBoolean(showLineNo);

        out.writeUTF(encodeName);
        out.writeBoolean(viewerParmsInitRequired);
        out.writeBoolean(viewerParmsRestoreRequired);

        out.writeInt(adapterFindPosition);
        out.writeUTF(adapterFindString);

    }

}
