package com.example.drrbnicompany.Fragments.EditProfile;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.drrbnicompany.Models.Company;
import com.example.drrbnicompany.R;
import com.example.drrbnicompany.ViewModels.EditProfileViewModel;
import com.example.drrbnicompany.ViewModels.MyListener;
import com.example.drrbnicompany.ViewModels.ProfileViewModel;
import com.example.drrbnicompany.databinding.FragmentEditProfileBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private FirebaseAuth auth;
    private EditProfileViewModel editProfileViewModel;
    private ActivityResultLauncher<String> getImg;
    private ActivityResultLauncher<String> permission;
    private Uri image;
    private Company thiCompany;

    public EditProfileFragment() {
    }

    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        editProfileViewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        editProfileViewModel.requestProfileInfo(auth.getCurrentUser().getUid());

        getImg = registerForActivityResult(
                new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            binding.profileImage.setImageURI(result);
                            image = result;
                        }
                    }
                });

        permission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result)
                            getImg.launch("image/*");

                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(getLayoutInflater(), container, false);

        load();
        getCategoriesName();

        editProfileViewModel.getProfileInfo().observe(requireActivity(), new Observer<Company>() {
            @Override
            public void onChanged(Company company) {
                if (getActivity() == null) return;
                thiCompany = company;
                if (company.getImg() == null) {
                    binding.profileImage.setImageResource(R.drawable.company_defult_image);
                } else {
                    Glide.with(getActivity()).load(company.getImg()).placeholder(R.drawable.anim_progress).into(binding.profileImage);
                }
                binding.editProfileEtName.setText(company.getName());
                binding.editProfileEtEmail.setText(company.getEmail());
                stopLoad();
            }
        });

        editProfileViewModel.getCategoriesName(new MyListener<List<String>>() {
            @Override
            public void onValuePosted(List<String> value) {
                int position=0;
                checkCategoryName(position, thiCompany);
                stopLoad();
            }
        });


        binding.tvAddImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });


        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                update();

                String name = binding.editProfileEtName.getText().toString().trim();
                String email = binding.editProfileEtEmail.getText().toString().trim();
                String category = binding.editProfileSpCategory.getSelectedItem().toString();

                //TODO: ليش هدول
                if (TextUtils.isEmpty(name))
                    name = thiCompany.getName();
                else if (TextUtils.isEmpty(email))
                    email = thiCompany.getEmail();
                else if (binding.editProfileSpCategory.getSelectedItemPosition() < 1)
                    category = thiCompany.getCategory();

                if (image == null) {
                    editProfileViewModel.editProfileDataWithoutImage(name, email, category, new MyListener<Boolean>() {
                        @Override
                        public void onValuePosted(Boolean value) {
                            if (value) {
                                Snackbar.make(view, "تم التعديل بنجاح", Snackbar.LENGTH_LONG).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                                stopUpdate();
                            }
                        }
                    }, new MyListener<Boolean>() {
                        @Override
                        public void onValuePosted(Boolean value) {
                            if (value) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                                Snackbar.make(view, "فشل التعديل", Snackbar.LENGTH_LONG).show();
                                stopUpdate();
                            }
                        }
                    });
                } else {

                    editProfileViewModel.editProfileData(image, name, email, category, new MyListener<Boolean>() {
                        @Override
                        public void onValuePosted(Boolean value) {
                            if (value) {
                                Snackbar.make(view, "تم التعديل بنجاح", Snackbar.LENGTH_LONG).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                                stopUpdate();
                            }
                        }
                    }, new MyListener<Boolean>() {
                        @Override
                        public void onValuePosted(Boolean value) {
                            if (value) {
                                Snackbar.make(view, "فشل التعديل", Snackbar.LENGTH_LONG).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                                stopUpdate();
                            }
                        }
                    });
                }
            }
        });

        binding.tvEditContactInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(binding.getRoot());
                navController.navigate(R.id.action_editProfileFragment_to_editContactInformationFragment);
            }
        });
        binding.tvEditAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(binding.getRoot());
                navController.navigate(R.id.action_editProfileFragment_to_editAddressFragment);
            }
        });
        binding.tvChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChangePasswordBottomSheetFragment bottomSheet =
                        ChangePasswordBottomSheetFragment.newInstance();
                bottomSheet.show(getFragmentManager(), "tag");
            }
        });

        return binding.getRoot();
    }

    private void checkCategoryName(int position, Company category) {
        if (category.getCategory().equals("أختر التخصص")) {
            position = 0;
            category.setCategory("أختر التصنيف");
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("التسويق")) {
            position = 1;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("تكنولوجيا المعلومات")) {
            position = 2;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("الهندسة")) {
            position = 3;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("البلديات")) {
            position = 4;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("التصميم والديكور")) {
            position = 5;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("المحاسبة")) {
            position = 6;
            binding.editProfileSpCategory.setSelection(position);
        } else if (category.getCategory().equals("الصحافة والاعلام")) {
            position = 7;
            binding.editProfileSpCategory.setSelection(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void getCategoriesName() {
        editProfileViewModel.getCategoriesName(new MyListener<List<String>>() {
            @Override
            public void onValuePosted(List<String> value) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, value);
                binding.editProfileSpCategory.setAdapter(adapter);
                stopLoad();
            }
        });
    }

    public void load() {
        binding.shimmerView.setVisibility(View.VISIBLE);
        binding.shimmerView.startShimmerAnimation();
        binding.editProfileLayout.setVisibility(View.GONE);
    }

    public void stopLoad() {
        binding.shimmerView.setVisibility(View.GONE);
        binding.shimmerView.stopShimmerAnimation();
        binding.editProfileLayout.setVisibility(View.VISIBLE);
    }


    public void update() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnOk.setEnabled(false);
        binding.btnOk.setClickable(false);
    }

    public void stopUpdate() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnOk.setEnabled(true);
        binding.btnOk.setClickable(true);
    }

}