import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface Profile {
    id: string;
    userId: string;
    type: 'INDIVIDUAL' | 'BUSINESS';
    name: string;
    ssnOrEin: string;
    businessName?: string;
    fiscalYearEnd?: string;
    address: {
        street: string;
        city: string;
        state: string;
        zip: string;
        country: string;
    };
    relationshipToUser?: string;
    isPrimary: boolean;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

interface ProfileContextType {
    profiles: Profile[];
    activeProfile: Profile | null;
    isLoading: boolean;
    fetchProfiles: () => Promise<void>;
    setActiveProfile: (profile: Profile) => void;
    createProfile: (profileData: any) => Promise<Profile>;
    updateProfile: (profileId: string, profileData: any) => Promise<Profile>;
    deleteProfile: (profileId: string) => Promise<void>;
}

const ProfileContext = createContext<ProfileContextType | undefined>(undefined);

export const useProfiles = () => {
    const context = useContext(ProfileContext);
    if (!context) {
        throw new Error('useProfiles must be used within a ProfileProvider');
    }
    return context;
};

interface ProfileProviderProps {
    children: ReactNode;
    token: string | null;
}

export const ProfileProvider: React.FC<ProfileProviderProps> = ({ children, token }) => {
    const [profiles, setProfiles] = useState<Profile[]>([]);
    const [activeProfile, setActiveProfileState] = useState<Profile | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (token) {
            fetchProfiles();
        }
    }, [token]);

    const fetchProfiles = async () => {
        if (!token) return;

        setIsLoading(true);
        try {
            const response = await fetch('/api/v1/users/profiles', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                setProfiles(data);

                // Set primary profile as active by default
                const primaryProfile = data.find((p: Profile) => p.isPrimary);
                if (primaryProfile && !activeProfile) {
                    setActiveProfileState(primaryProfile);
                    localStorage.setItem('active_profile_id', primaryProfile.id);
                }
            }
        } catch (error) {
            console.error('Failed to fetch profiles:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const setActiveProfile = (profile: Profile) => {
        setActiveProfileState(profile);
        localStorage.setItem('active_profile_id', profile.id);
    };

    const createProfile = async (profileData: any): Promise<Profile> => {
        if (!token) throw new Error('Not authenticated');

        const response = await fetch('/api/v1/users/profiles', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(profileData)
        });

        if (!response.ok) {
            throw new Error('Failed to create profile');
        }

        const newProfile = await response.json();
        setProfiles([...profiles, newProfile]);
        return newProfile;
    };

    const updateProfile = async (profileId: string, profileData: any): Promise<Profile> => {
        if (!token) throw new Error('Not authenticated');

        const response = await fetch(`/api/v1/users/profiles/${profileId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(profileData)
        });

        if (!response.ok) {
            throw new Error('Failed to update profile');
        }

        const updatedProfile = await response.json();
        setProfiles(profiles.map(p => p.id === profileId ? updatedProfile : p));

        if (activeProfile?.id === profileId) {
            setActiveProfileState(updatedProfile);
        }

        return updatedProfile;
    };

    const deleteProfile = async (profileId: string): Promise<void> => {
        if (!token) throw new Error('Not authenticated');

        const response = await fetch(`/api/v1/users/profiles/${profileId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to delete profile');
        }

        setProfiles(profiles.filter(p => p.id !== profileId));

        if (activeProfile?.id === profileId) {
            const primaryProfile = profiles.find(p => p.isPrimary);
            setActiveProfileState(primaryProfile || null);
        }
    };

    const value: ProfileContextType = {
        profiles,
        activeProfile,
        isLoading,
        fetchProfiles,
        setActiveProfile,
        createProfile,
        updateProfile,
        deleteProfile
    };

    return <ProfileContext.Provider value={value}>{children}</ProfileContext.Provider>;
};
