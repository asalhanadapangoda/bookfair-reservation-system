import client from './client';
import { resolveRoleByEmail } from './dashboardApi';

/**
 * Log in the user
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise<Object>} Response data
 */
const decodeToken = (token) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
};

/**
 * Log in the user
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise<Object>} Response data
 */
export const loginUser = async (email, password) => {
    try {
        const response = await client.post('/auth/login', { email, password });

        if (response.data.token) {
            // Token is now set securely via HttpOnly cookie in the backend response
            const decoded = decodeToken(response.data.token);
            if (decoded) {
                const userData = {
                    businessName: response.data.businessName || decoded.name || '',
                    contactPerson: response.data.contactPerson || decoded.name || '',
                    email: decoded.email || email,
                    role: decoded.role,
                    userId: decoded.userId || response.data.userId,
                };
                localStorage.setItem('user', JSON.stringify(userData));
            } else {
                const roleData = await resolveRoleByEmail(email);
                const userData = {
                    businessName: response.data.businessName ?? '',
                    contactPerson: response.data.contactPerson ?? '',
                    email,
                    role: roleData.role,
                    userId: response.data.userId || roleData.userId,
                };
                localStorage.setItem('user', JSON.stringify(userData));
            }
        }

        return { success: true, data: response.data, user: JSON.parse(localStorage.getItem('user')) };
    } catch (error) {
        console.error("Login Error Details:", error);
        if (error.response) {
            console.error("Response Data:", error.response.data);
            console.error("Response Status:", error.response.status);
        }
        // Extract error message from backend response if available
        const message = error.response?.data?.message || 'Login failed. Please check your credentials.';
        throw new Error(message);
    }
};

/**
 * Register a new user
 * @param {Object} userData - { name, email, password }
 * @returns {Promise<Object>} Response data
 */
export const registerUser = async (userData) => {
    try {
        const response = await client.post('/auth/register', userData);
        return { success: true, data: response.data };
    } catch (error) {
        const message = error.response?.data?.message || 'Registration failed. Please try again.';
        throw new Error(message);
    }
};

/**
 * Verify if email exists
 * @param {string} email 
 * @returns {Promise<boolean>}
 */
export const verifyEmail = async (email) => {
    try {
        console.log("Calling verifyEmail for:", email);
        const response = await client.post('/auth/verify-email', { email });
        return response.data;
    } catch (error) {
        const message = error.response?.data?.message || 'Verification failed.';
        throw new Error(message);
    }
};

/**
 * Request password reset (sends OTP)
 * @param {string} email 
 * @returns {Promise<Object>}
 */
export const forgotPassword = async (email) => {
    try {
        const response = await client.post('/auth/forgot-password', { email });
        return { success: true, data: response.data };
    } catch (error) {
        const message = error.response?.data?.message || 'Failed to send OTP.';
        throw new Error(message);
    }
};

/**
 * Verify OTP
 * @param {string} email 
 * @param {string} otp 
 * @returns {Promise<Object>}
 */
export const verifyOtp = async (email, otp) => {
    try {
        const response = await client.post('/auth/verify-otp', { email, otp });
        return { success: true, data: response.data };
    } catch (error) {
        const message = error.response?.data?.message || 'Invalid or expired OTP.';
        throw new Error(message);
    }
};

/**
 * Reset password
 * @param {string} email 
 * @param {string} otp
 * @param {string} newPassword 
 * @returns {Promise<Object>} Response data
 */
export const resetPasswordNew = async (email, otp, newPassword) => {
    try {
        const response = await client.post('/auth/reset-password', { email, otp, newPassword });
        return { success: true, data: response.data };
    } catch (error) {
        const message = error.response?.data?.message || 'Reset failed.';
        throw new Error(message);
    }
};

/**
 * Logout the user
 */
export const logoutUser = async () => {
    try {
        await client.post('/auth/logout');
    } catch (error) {
        console.error("Logout failed:", error);
    }
    localStorage.removeItem('user');
};
/**
 * Fetch authenticated user from backend claims
 */
export const fetchMe = async () => {
    try {
        const response = await client.get('/auth/me');
        if (response.data) {
            const userData = {
                email: response.data.email,
                role: response.data.role,
                userId: response.data.userId,
                name: response.data.name,
                businessName: response.data.name, // Fallback
                contactPerson: response.data.name, // Fallback
            };
            localStorage.setItem('user', JSON.stringify(userData));
            return userData;
        }
    } catch (error) {
        console.error("fetchMe failed", error);
        localStorage.removeItem('user');
    }
    return null;
};
