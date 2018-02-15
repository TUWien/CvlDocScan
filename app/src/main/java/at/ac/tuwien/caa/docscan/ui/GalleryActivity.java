package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.fivehundredpx.greedolayout.GreedoLayoutManager;
import com.fivehundredpx.greedolayout.GreedoSpacingItemDecoration;
import com.fivehundredpx.greedolayout.RowLayoutManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.GalleryAdapter;
import at.ac.tuwien.caa.docscan.gallery.GalleryTestAdapter;
import at.ac.tuwien.caa.docscan.gallery.SimpleItemTouchHelperCallback;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;

/**
 * Created by fabian on 01.02.2018.
 */

public class GalleryActivity extends AppCompatActivity implements
        GalleryAdapter.GalleryAdapterCallback {

    private Toolbar mToolbar;
    private AppBarLayout.LayoutParams mAppBarLayoutParams;
    private CoordinatorLayout.LayoutParams mCoordinatorLayoutParams;
    private AppBarLayout mAppBarLayout;
    private Document mDocument;
    private Menu mMenu;
    private GalleryAdapter mAdapter;
//    private GalleryTestAdapter mAdapter;

    private RecyclerView mRecyclerView;
    private String mFileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);

        //        dummy document - start
        mFileName = getIntent().getStringExtra("DOCUMENT_FILE_NAME");
        if (mFileName == null)
            mFileName = "/storage/emulated/0/Pictures/DocScan/default";

//        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);
//        mRecyclerView.setHasFixedSize(true);
//        mRecyclerView.setLayoutManager(layoutManager);

////        RecyclerView.LayoutManager layoutManager = new RowLayoutManager();
//        GreedoLayoutManager layoutManager = new GreedoLayoutManager(recyclerAdapter);
//        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);
//        mRecyclerView.setLayoutManager(layoutManager);



    }

    @Override
    protected void onResume() {
        super.onResume();

        loadDocument();
        initToolbar();
        initAdapter();

    }

    private void initAdapter() {

        mAdapter = new GalleryAdapter(this, mDocument);
//        mAdapter = new GalleryTestAdapter(this, mDocument);
        mAdapter.setFileName(mFileName);
        mRecyclerView.setAdapter(mAdapter);
//        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(3, 1);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 10);
        mRecyclerView.setLayoutManager(layoutManager);



//        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
//        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
//        touchHelper.attachToRecyclerView(mRecyclerView);

//        mAdapter = new GalleryTestAdapter(this, mDocument);
////        mAdapter = new GalleryTestAdapter(this, mDocument);
//        mAdapter.setFileName(mFileName);
//        mRecyclerView.setAdapter(mAdapter);
//
//        //        RecyclerView.LayoutManager layoutManager = new RowLayoutManager();
//        RowLayoutManager layoutManager = new RowLayoutManager(mAdapter);
//        layoutManager.setMaxRowHeight(dpToPx(200, this));
////        layoutManager.setFixedHeight(true);
//        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);
//        mRecyclerView.setLayoutManager(layoutManager);
//        int spacing = dpToPx(1, this);
//        mRecyclerView.addItemDecoration(new GreedoSpacingItemDecoration(spacing));



    }

    public static int pxToDp(int px, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px,
                context.getResources().getDisplayMetrics());
    }

    public static int dpToPx(float dp, Context context) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    private void loadDocument() {
        Document document = new Document();
        ArrayList<File> fileList = getFileList(mFileName);
        ArrayList<Page> pages = filesToPages(fileList);
        document.setPages(pages);

        File file = new File(mFileName);
        document.setTitle(file.getName());

        mDocument = document;
    }

    private void initToolbar() {

        mToolbar = findViewById(R.id.main_toolbar);
        mAppBarLayout = findViewById(R.id.gallery_appbar);
        mToolbar.setTitle(mDocument.getTitle());

        mAppBarLayoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        mCoordinatorLayoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();

//        Enable back navigation in action bar:
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

//        switch (item.getItemId()){
//            case android.R.id.home:
//                if (mAdapter.getSelectionCount() == 0) {
//                    onBackPressed();
//                    return true;
//                }
//                else {
//                    mAdapter.deselectAllItems();
//                    return true;
//                }
//        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_menu, menu);

//        mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
//        mMenu.setGroupVisible(R.id.gallery_menu_main, true);

        mMenu = menu;

        return true;

    }

    public void selectAllItems(MenuItem item) {

//        mAdapter.selectAllItems();

     }

    public void deleteSelectedItems(MenuItem item) {

        showDeleteConfirmationDialog();

    }

    public void rotateSelectedItems(MenuItem item) {

//        if (mDocument == null || mAdapter == null)
//            return;
//
//        int[] selections = mAdapter.getSelectionIndices();
//
//        for (int i = 0; i < selections.length; i++) {
//            try {
//                Helper.rotateExif(mDocument.getPages().get(selections[i]).getFile());
//                mAdapter.notifyItemChanged(selections[i]);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

    }

    private void deleteSelections() {

//        if (mDocument == null || mAdapter == null)
//            return;
//
//        int[] selections = mAdapter.getSelectionIndices();
//
//        for (int i = selections.length - 1; i >= 0; i--) {
//            mDocument.getPages().get(selections[i]).getFile().delete();
//            mDocument.getPages().remove(selections[i]);
//        }
//
//        mAdapter.clearSelection();
//        mAdapter.notifyDataSetChanged();

    }


    @Override
    public void onSelectionChange(int selectionCount) {

        // No selection - let the toolbar disappear, after scrolling down:
        if (selectionCount == 0)
            fixToolbar();
        // One or more items are selected - the toolbar stays:
        else
            scrollToolbar(selectionCount);

    }

    @Override
    public void onDragStart() {

//        int[] selections = mAdapter.getSelectionIndices();
//
//        for (int i = selections.length - 1; i >= 1; i--) {
////            mDocument.getPages().get(selections[i]).getFile().delete();
//            mDocument.getPages().remove(selections[i]);
//        }
//
//        mAdapter.clearSelection();
//        mAdapter.notifyDataSetChanged();

    }

    private void scrollToolbar(int selectionCount) {

        // This is based on post of Denny Weinberg:
        // https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
        mAppBarLayoutParams.setScrollFlags(0);
        mCoordinatorLayoutParams.setBehavior(null);

//            Set the action bar title:
        mToolbar.setTitle(Integer.toString(selectionCount) + " "
                + getResources().getString(R.string.gallery_selected));
//            Set custom home button:
        mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_clear_material);

//            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mMenu.setGroupVisible(R.id.gallery_menu_main, false);
        mMenu.setGroupVisible(R.id.gallery_menu_selection, true);
        mToolbar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);

    }

    private void fixToolbar() {

        // This is based on post of Denny Weinberg:
        // https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
        mAppBarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        mCoordinatorLayoutParams.setBehavior(new AppBarLayout.Behavior());

//            Set the action bar title:
        mToolbar.setTitle(mDocument.getTitle());
        mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//            Set the default back button:
        mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);

        mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
        mMenu.setGroupVisible(R.id.gallery_menu_main, true);
        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);

    }

    private void showDeleteConfirmationDialog() {


//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//
//        String title = getResources().getString(R.string.gallery_confirm_delete_title_prefix);
//        title += " " + mAdapter.getSelectionCount();
//        if (mAdapter.getSelectionCount() == 1)
//            title += " " + getResources().getString(R.string.gallery_confirm_delete_title_single_postfix);
//        else
//            title += " " + getResources().getString(R.string.gallery_confirm_delete_title_multiple_postfix);
//
//        // set dialog message
//        alertDialogBuilder
//                .setMessage(R.string.gallery_confirm_delete_text)
//                .setTitle(title)
//                .setPositiveButton(R.string.gallery_confirm_delete_confirm_button_text, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i)  {
//                        deleteSelections();
//                    }
//                })
//                .setNegativeButton(R.string.gallery_confirm_delete_cancel_button_text, null)
//                .setCancelable(true);
////                .setMessage(deleteText);
//
//        // create alert dialog
//        AlertDialog alertDialog = alertDialogBuilder.create();
//
//        // show it
//        alertDialog.show();

    }


//    TODO: temporary helper methods copied from BaseDocumentAdapter. Replace them.

    private ArrayList<File> getFileList(String dir) {

        File[] files = getFiles(new File(dir));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<File> getFileList(Context context) {

        File mediaStorageDir = Helper.getMediaStorageDir(context.getResources().getString(R.string.app_name));

        if (mediaStorageDir == null)
            return null;

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] dirs = mediaStorageDir.listFiles(directoryFilter);
        File[] files = getFiles(dirs[27]);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    private File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }

    //   END of temporary helper methods copied from BaseDocumentAdapter. Replace them.



}