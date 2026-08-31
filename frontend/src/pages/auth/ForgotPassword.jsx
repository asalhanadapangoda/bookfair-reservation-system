import React, { useState } from 'react';
import { Snackbar, Alert } from '@mui/material';

// Components
import AuthLayout from '../../components/layout/AuthLayout';
import ForgotPasswordForm from '../../components/auth/ForgotPasswordForm';

// Utils & API
import { validateEmail, validatePassword, validatePasswordMatch } from '../../utils/validation';
import { forgotPassword, verifyOtp, resetPasswordNew } from '../../api/authApi';

// Assets
const forgotPasswordImage = '/assets/login_pic.jpg'; // Using same image for now

const ForgotPassword = () => {
    const [step, setStep] = useState(1); // 1: Email, 2: OTP, 3: New Password, 4: Success
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [passwords, setPasswords] = useState({ password: '', confirmPassword: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

    const handleChange = (e) => {
        const { name, value } = e.target;
        if (name === 'email') {
            setEmail(value);
        } else if (name === 'otp') {
            setOtp(value);
        } else {
            setPasswords({ ...passwords, [name]: value });
        }
        if (error) setError('');
    };

    const handleEmailSubmit = async (e) => {
        e.preventDefault();
        const emailError = validateEmail(email);
        if (emailError) {
            setError(emailError);
            return;
        }

        setLoading(true);
        try {
            await forgotPassword(email);
            setStep(2);
            setSnackbar({ open: true, message: 'OTP sent to your email.', severity: 'success' });
        } catch (err) {
            setSnackbar({ open: true, message: err.message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleOtpSubmit = async (e) => {
        e.preventDefault();
        if (!otp || otp.length < 6) {
            setError('Please enter a valid 6-digit OTP.');
            return;
        }

        setLoading(true);
        try {
            await verifyOtp(email, otp);
            setStep(3);
            setSnackbar({ open: true, message: 'OTP verified. Please set your new password.', severity: 'success' });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleResetSubmit = async (e) => {
        e.preventDefault();

        const passwordError = validatePassword(passwords.password);
        if (passwordError) {
            setError(passwordError);
            return;
        }

        const matchError = validatePasswordMatch(passwords.password, passwords.confirmPassword);
        if (matchError) {
            setError(matchError);
            return;
        }

        setLoading(true);
        try {
            await resetPasswordNew(email, otp, passwords.password);
            setStep(4);
            setSnackbar({ open: true, message: 'Password reset successfully!', severity: 'success' });
        } catch (err) {
            setSnackbar({ open: true, message: err.message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleCloseSnackbar = () => setSnackbar({ ...snackbar, open: false });

    return (
        <AuthLayout
            image={forgotPasswordImage}
            title="Recovery"
            subtitle={step === 1 ? "Enter your email to receive an OTP." : step === 2 ? "Enter the OTP sent to your email." : "Set your new account password."}
        >
            <ForgotPasswordForm
                step={step}
                email={email}
                otp={otp}
                passwords={passwords}
                handleChange={handleChange}
                handleSubmit={step === 1 ? handleEmailSubmit : step === 2 ? handleOtpSubmit : handleResetSubmit}
                error={error}
                loading={loading}
            />

            <Snackbar
                open={snackbar.open}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
                <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </AuthLayout>
    );
};

export default ForgotPassword;
