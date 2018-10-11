package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 22.08.2017.
 */

public class AccountActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        initButtons();

    }

    @Override
    protected void onResume() {

        super.onResume();

        showAccountDetails();
        showButtonsLayout(Helper.isOnline(this));

    }

    private void showButtonsLayout(boolean show) {

        LinearLayout buttonsLayout = findViewById(R.id.account_button_layout);
        TextView offlineTextView = findViewById(R.id.account_offline_textview);
        if (show) {
            buttonsLayout.setVisibility(View.VISIBLE);
            offlineTextView.setVisibility(View.GONE);
        }
        else {
            buttonsLayout.setVisibility(View.GONE);
            offlineTextView.setVisibility(View.VISIBLE);
        }

    }

    private void initButtons() {
        Button transkribusButton = (Button) findViewById(R.id.sync_switcher_transkribus_button);
        transkribusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        Button dropboxButon = (Button) findViewById(R.id.sync_switcher_dropbox_button);
        dropboxButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DropboxActivity.class);
                intent.putExtra("test", "withinapp");
                startActivity(intent);
            }
        });
    }

    private void showAccountDetails() {

        // Show the first and last name of the user:
        TextView userTextView = (TextView) findViewById(R.id.account_user_textview);
        String userText = getString(R.string.account_user_textview_text) + " " +
                User.getInstance().getFirstName() + " " + User.getInstance().getLastName();
        userTextView.setText(userText);

        // Show the cloud service:
        TextView cloudTextView = (TextView) findViewById(R.id.account_cloud_textview);
        String cloudText = getString(R.string.account_cloud_textview_text) + " ";
        if (User.getInstance().getConnection() == User.SYNC_DROPBOX)
            cloudText += getString(R.string.sync_dropbox_text);
        else if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS)
            cloudText += getString(R.string.sync_transkribus_text);
        cloudTextView.setText(cloudText);

    }

    private void hideAccountDetails() {
//            Remove the view from the parent layout. If the view is just hidden, the layout below
//            is not centered.
        View v = findViewById(R.id.current_account_layout);
        if (v != null)
            ((ViewManager)v.getParent()).removeView(v);
    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ACCOUNT_EDIT;
    }



}
