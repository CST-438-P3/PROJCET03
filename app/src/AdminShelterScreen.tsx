import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ApiError } from './api';
import { AdminApi } from './adminApi';
import { addressLine, initials, tintFor } from './adminFormat';
import { adminStyles } from './adminStyles';
import { ShelterFormSheet } from './ShelterFormSheet';
import { color, font } from './theme';
import { Shelter, StaffMember } from './types';

const STAFF_ERROR = "We couldn't load the staff. Please try again.";
const CHANGE_ERROR = "That didn't work. Please try again.";
const TOAST_MS = 3000;

type AdminShelterScreenProps = {
  shelter: Shelter;
  // Goes back to the shelter list. AdminScreen also sends Android's back button here.
  onBack: () => void;
  api: AdminApi;
};

/** One shelter for an admin: its details, its staff, and edit and delete (S10). */
export function AdminShelterScreen({ shelter: first, onBack, api }: AdminShelterScreenProps) {
  const [shelter, setShelter] = useState(first);
  const [staff, setStaff] = useState<StaffMember[] | null>(null);
  const [staffError, setStaffError] = useState<string | null>(null);
  const [userId, setUserId] = useState('');
  const [addError, setAddError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadStaff = useCallback(async () => {
    setStaffError(null);
    try {
      setStaff(await api.listStaff(shelter.id));
    } catch {
      setStaffError(STAFF_ERROR);
    }
  }, [api, shelter.id]);

  useEffect(() => {
    void loadStaff();
  }, [loadStaff]);

  // Stops the toast timer when the screen closes.
  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
  }, []);

  function showToast(message: string) {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast(message);
    toastTimer.current = setTimeout(() => setToast(null), TOAST_MS);
  }

  async function add() {
    const id = userId.trim();
    if (!/^\d+$/.test(id)) {
      setAddError('Enter a user ID, numbers only');
      return;
    }
    setBusy(true);
    setAddError(null);
    try {
      await api.addStaff(shelter.id, Number(id));
      setUserId('');
      showToast(`User ${id} can now post for ${shelter.name}`);
      await loadStaff();
    } catch (e) {
      setAddError(e instanceof ApiError && e.problem.status === 404 && e.problem.detail ? e.problem.detail : CHANGE_ERROR);
    } finally {
      setBusy(false);
    }
  }

  async function remove(member: StaffMember) {
    setBusy(true);
    setAddError(null);
    try {
      await api.removeStaff(shelter.id, member.id);
      showToast(`${member.email} can no longer post for ${shelter.name}`);
      await loadStaff();
    } catch {
      setAddError(CHANGE_ERROR);
    } finally {
      setBusy(false);
    }
  }

  async function deleteShelter() {
    setBusy(true);
    setDeleteError(null);
    try {
      await api.deleteShelter(shelter.id);
      onBack();
    } catch {
      setDeleteError(CHANGE_ERROR);
      setBusy(false);
    }
  }

  return (
    <SafeAreaView edges={['top']} role="main" style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.hero}>
          <View style={styles.nav}>
            <Pressable role="button" accessibilityLabel="Back to shelters" onPress={onBack} style={styles.navButton}>
              <Text style={styles.backArrow}>←</Text>
            </Pressable>
            <Pressable role="button" accessibilityLabel="Edit shelter" onPress={() => setEditing(true)} style={styles.navButton}>
              <Text style={styles.edit}>Edit</Text>
            </Pressable>
          </View>
          <View style={styles.identity}>
            <View style={styles.bigAvatar}>
              <Text style={styles.bigAvatarText}>{initials(shelter.name)}</Text>
            </View>
            <View style={styles.grow}>
              <Text role="heading" aria-level={1} style={styles.title}>{shelter.name}</Text>
              <Text style={styles.address}>{addressLine(shelter.address, shelter.zip)}</Text>
            </View>
          </View>
          <View style={styles.chips}>
            {!!shelter.phone && <Text style={styles.chip}>☎ {shelter.phone}</Text>}
            {!!shelter.email && <Text style={styles.chip}>✉ {shelter.email}</Text>}
          </View>
        </View>

        <View style={styles.body}>
          <View style={[adminStyles.card, styles.panel]}>
            <View style={styles.panelHeader}>
              <Text role="heading" aria-level={2} style={styles.panelTitle}>Staff</Text>
              {staff && <Text style={styles.count}>{staff.length}</Text>}
            </View>
            {staffError && (
              <View style={styles.inline}>
                <Text role="alert" style={adminStyles.errorText}>{staffError}</Text>
                <Pressable role="button" onPress={() => void loadStaff()}>
                  <Text style={styles.link}>Try again</Text>
                </Pressable>
              </View>
            )}
            {!staffError && !staff && <ActivityIndicator accessibilityLabel="Loading staff" color={color.ink} />}
            {staff && staff.length === 0 && <Text style={styles.muted}>No staff yet. Only staff can post for this shelter.</Text>}
            {staff?.map((member, index) => (
              <View key={member.id} style={[styles.person, index > 0 && styles.personLine]}>
                <View style={[styles.personAvatar, { backgroundColor: tintFor(member.id) }]}>
                  <Text style={styles.personAvatarText}>{initials(member.displayName ?? member.email)}</Text>
                </View>
                <View style={styles.grow}>
                  <Text style={styles.personName}>{member.email}</Text>
                  <Text style={styles.muted}>User {member.id}</Text>
                </View>
                <Pressable
                  role="button"
                  accessibilityLabel={`Remove ${member.email}`}
                  disabled={busy}
                  onPress={() => void remove(member)}
                  style={styles.remove}
                >
                  <Text style={styles.removeText}>✕</Text>
                </Pressable>
              </View>
            ))}
            <View style={styles.addRow}>
              <TextInput
                accessibilityLabel="User ID"
                value={userId}
                onChangeText={setUserId}
                placeholder="User ID"
                placeholderTextColor={color.muted}
                keyboardType="number-pad"
                style={[adminStyles.input, styles.userIdInput, !!addError && adminStyles.inputError]}
              />
              <Pressable role="button" accessibilityLabel="Add staff" disabled={busy} onPress={() => void add()} style={styles.addButton}>
                <Text style={styles.addText}>Add</Text>
              </Pressable>
            </View>
            {!!addError && <Text style={adminStyles.errorText}>{addError}</Text>}
          </View>

          {!confirmDelete ? (
            <Pressable role="button" onPress={() => setConfirmDelete(true)} style={styles.dangerButton}>
              <Text style={styles.dangerText}>Delete shelter</Text>
            </Pressable>
          ) : (
            <View style={styles.confirm}>
              <Text style={styles.confirmText}>Delete {shelter.name}? Its posts are deleted too.</Text>
              <View style={styles.confirmRow}>
                <Pressable role="button" onPress={() => setConfirmDelete(false)} style={styles.cancel}>
                  <Text style={styles.cancelText}>Cancel</Text>
                </Pressable>
                <Pressable role="button" accessibilityLabel="Yes, delete" disabled={busy} onPress={() => void deleteShelter()} style={styles.confirmDelete}>
                  <Text style={styles.confirmDeleteText}>Delete</Text>
                </Pressable>
              </View>
              {!!deleteError && <Text role="alert" style={adminStyles.errorText}>{deleteError}</Text>}
            </View>
          )}
        </View>
      </ScrollView>

      {toast && (
        <View role="status" style={styles.toast}>
          <Text style={styles.toastDot}>✓</Text>
          <Text style={styles.toastText}>{toast}</Text>
        </View>
      )}

      {editing && (
        <ShelterFormSheet
          title="Edit shelter"
          submitLabel="Save changes"
          initial={{
            name: shelter.name,
            address: shelter.address,
            zip: shelter.zip,
            phone: shelter.phone ?? '',
            email: shelter.email ?? '',
          }}
          onClose={() => setEditing(false)}
          onSubmit={async (input) => {
            setShelter(await api.updateShelter(shelter.id, input));
            setEditing(false);
            showToast('Saved');
          }}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.background },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center', paddingBottom: 100 },
  hero: {
    backgroundColor: color.brand,
    paddingHorizontal: 22,
    paddingTop: 12,
    paddingBottom: 22,
    borderBottomLeftRadius: 28,
    borderBottomRightRadius: 28,
    gap: 14,
  },
  nav: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  navButton: { minWidth: 44, height: 44, justifyContent: 'center' },
  backArrow: { color: color.ink, fontSize: 26, lineHeight: 30, marginLeft: -4 },
  edit: { color: color.ink, fontFamily: font.semiBold, fontSize: 16, textAlign: 'right' },
  identity: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  bigAvatar: { width: 62, height: 62, borderRadius: 20, backgroundColor: color.surface, alignItems: 'center', justifyContent: 'center' },
  bigAvatarText: { color: color.ink, fontFamily: font.extraBold, fontSize: 22 },
  grow: { flex: 1 },
  title: { color: color.ink, fontFamily: font.extraBold, fontSize: 26, lineHeight: 30 },
  address: { color: color.onSurfaceDisabled, fontFamily: font.regular, fontSize: 14, marginTop: 4 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    backgroundColor: 'rgba(255, 255, 255, 0.7)',
    borderRadius: 999,
    paddingVertical: 7,
    paddingHorizontal: 12,
    overflow: 'hidden',
    color: color.ink,
    fontFamily: font.semiBold,
    fontSize: 13,
  },
  body: { padding: 18, gap: 14 },
  panel: { padding: 14, gap: 4 },
  panelHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 },
  panelTitle: { color: color.ink, fontFamily: font.semiBold, fontSize: 17 },
  count: {
    backgroundColor: color.surfaceActive,
    borderRadius: 999,
    paddingVertical: 3,
    paddingHorizontal: 10,
    overflow: 'hidden',
    color: color.ink,
    fontFamily: font.semiBold,
    fontSize: 13,
  },
  inline: { flexDirection: 'row', gap: 10, alignItems: 'center', flexWrap: 'wrap' },
  link: { color: color.ink, fontFamily: font.semiBold, fontSize: 14, textDecorationLine: 'underline' },
  muted: { color: color.muted, fontFamily: font.regular, fontSize: 13 },
  person: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 8 },
  personLine: { borderTopWidth: 1, borderTopColor: '#EEF3F6' },
  personAvatar: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  personAvatarText: { color: color.ink, fontFamily: font.semiBold, fontSize: 14 },
  personName: { color: color.ink, fontFamily: font.semiBold, fontSize: 15 },
  remove: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  removeText: { color: color.muted, fontSize: 18 },
  addRow: { flexDirection: 'row', gap: 8, marginTop: 6 },
  userIdInput: { flex: 1, paddingVertical: 10 },
  addButton: { height: 44, paddingHorizontal: 20, borderRadius: 22, backgroundColor: color.ink, justifyContent: 'center' },
  addText: { color: color.surface, fontFamily: font.semiBold, fontSize: 15 },
  dangerButton: { minHeight: 44, alignItems: 'center', justifyContent: 'center' },
  dangerText: { color: color.danger, fontFamily: font.semiBold, fontSize: 15 },
  confirm: { backgroundColor: color.dangerSurface, borderRadius: 20, padding: 16, gap: 12 },
  confirmText: { color: color.ink, fontFamily: font.semiBold, fontSize: 15 },
  confirmRow: { flexDirection: 'row', gap: 10 },
  cancel: { flex: 1, minHeight: 44, borderRadius: 22, backgroundColor: color.surface, alignItems: 'center', justifyContent: 'center' },
  cancelText: { color: color.ink, fontFamily: font.semiBold, fontSize: 15 },
  confirmDelete: { flex: 1, minHeight: 44, borderRadius: 22, backgroundColor: color.danger, alignItems: 'center', justifyContent: 'center' },
  confirmDeleteText: { color: color.surface, fontFamily: font.semiBold, fontSize: 15 },
  toast: {
    position: 'absolute',
    left: 18,
    right: 18,
    bottom: 22,
    maxWidth: 684,
    alignSelf: 'center',
    backgroundColor: color.ink,
    borderRadius: 16,
    paddingVertical: 13,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  toastDot: {
    width: 22,
    height: 22,
    borderRadius: 11,
    overflow: 'hidden',
    backgroundColor: color.brand,
    color: color.ink,
    textAlign: 'center',
    lineHeight: 22,
    fontSize: 12,
    fontFamily: font.extraBold,
  },
  toastText: { flex: 1, color: color.surface, fontFamily: font.regular, fontSize: 14 },
});

