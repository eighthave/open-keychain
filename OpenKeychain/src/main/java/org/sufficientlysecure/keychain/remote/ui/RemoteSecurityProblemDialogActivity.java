/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.remote.ui;


import java.io.Serializable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.SecurityProblemPresenter.RemoteSecurityProblemView;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;


public class RemoteSecurityProblemDialogActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_SECURITY_PROBLEM = "security_problem";


    private SecurityProblemPresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.presenter = new SecurityProblemPresenter(getBaseContext());

        if (savedInstanceState == null) {
            RemoteRegisterDialogFragment frag = new RemoteRegisterDialogFragment();
            frag.show(getSupportFragmentManager(), "requestKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        Serializable keySecurityProblem = intent.getSerializableExtra(EXTRA_SECURITY_PROBLEM);

        presenter.setupFromIntentData(packageName, keySecurityProblem);
    }

    public static class RemoteRegisterDialogFragment extends DialogFragment {
        private SecurityProblemPresenter presenter;
        private RemoteSecurityProblemView mvpView;

        private Button buttonGotIt;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(theme).inflate(R.layout.remote_security_issue_dialog, null, false);
            alert.setView(view);

            buttonGotIt = (Button) view.findViewById(R.id.button_allow);

            setupListenersForPresenter();
            mvpView = createMvpView(view);

            return alert.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            presenter = ((RemoteSecurityProblemDialogActivity) getActivity()).presenter;
            presenter.setView(mvpView);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            if (presenter != null) {
                presenter.onCancel();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            if (presenter != null) {
                presenter.setView(null);
                presenter = null;
            }
        }

        @NonNull
        private RemoteSecurityProblemView createMvpView(View view) {
            final LinearLayout insecureWarningLayout = (LinearLayout) view.findViewById(R.id.insecure_warning_layout);
            final ImageView iconClientApp = (ImageView) view.findViewById(R.id.icon_client_app);
            final TextView explanationText = (TextView) insecureWarningLayout.findViewById(R.id.dialog_insecure_text);
            final TextView recommendText = (TextView) insecureWarningLayout.findViewById(R.id.dialog_insecure_recommend_text);
            final View recommendLayout = insecureWarningLayout.findViewById(R.id.dialog_insecure_recommend_layout);

            return new RemoteSecurityProblemView() {
                @Override
                public void finishAsCancelled() {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    activity.setResult(RESULT_CANCELED);
                    activity.finish();
                }

                @Override
                public void setTitleClientIcon(Drawable drawable) {
                    iconClientApp.setImageDrawable(drawable);
                }

                /* specialized layouts, for later?
                private void inflateWarningContentLayout(int dialog_insecure_mdc) {
                    insecureWarningLayout.removeAllViews();
                    getLayoutInflater(null).inflate(dialog_insecure_mdc, insecureWarningLayout);
                }
                */

                private void showGeneric(@StringRes int explanationStringRes) {
                    explanationText.setText(explanationStringRes);
                    recommendLayout.setVisibility(View.GONE);
                }

                private void showGenericWithRecommendation(
                        @StringRes int explanationStringRes, @StringRes int recommendationStringRes) {
                    explanationText.setText(explanationStringRes);
                    recommendText.setText(recommendationStringRes);
                    recommendLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void showLayoutMissingMdc() {
                    showGenericWithRecommendation(R.string.insecure_msg_mdc, R.string.insecure_recom_mdc);
                }

                @Override
                public void showLayoutInsecureSymmetric(int symmetricAlgorithm) {
                    showGeneric(R.string.insecure_msg_unidentified_key);
                }

                @Override
                public void showLayoutInsecureHashAlgorithm(int hashAlgorithm) {
                    showGeneric(R.string.insecure_msg_unidentified_key);
                }

                @Override
                public void showLayoutEncryptInsecureBitsize(int algorithmId, int bitStrength) {
                    String algorithmName = KeyFormattingUtils.getAlgorithmInfo(algorithmId, null, null);
                    explanationText.setText(
                            getString(R.string.insecure_msg_bitstrength, algorithmName,
                                    Integer.toString(bitStrength), "2010"));
                    recommendText.setText(R.string.insecure_recom_new_key);
                    recommendText.setVisibility(View.VISIBLE);
                }

                @Override
                public void showLayoutSignInsecureBitsize(int algorithmId, int bitStrength) {
                    String algorithmName = KeyFormattingUtils.getAlgorithmInfo(algorithmId, null, null);
                    explanationText.setText(
                            getString(R.string.insecure_msg_bitstrength, algorithmName,
                                    Integer.toString(bitStrength), "2010"));
                    recommendText.setText(R.string.insecure_recom_new_key);
                    recommendText.setVisibility(View.VISIBLE);
                }

                @Override
                public void showLayoutEncryptNotWhitelistedCurve(String curveOid) {
                    showGeneric(R.string.insecure_msg_not_whitelisted_curve);
                }

                @Override
                public void showLayoutSignNotWhitelistedCurve(String curveOid) {
                    showGeneric(R.string.insecure_msg_not_whitelisted_curve);
                }

                @Override
                public void showLayoutEncryptUnidentifiedKeyProblem() {
                    showGeneric(R.string.insecure_msg_unidentified_key);
                }

                @Override
                public void showLayoutSignUnidentifiedKeyProblem() {
                    showGeneric(R.string.insecure_msg_unidentified_key);
                }
            };
        }

        private void setupListenersForPresenter() {
            buttonGotIt.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickGotIt();
                }
            });
        }
    }

}
