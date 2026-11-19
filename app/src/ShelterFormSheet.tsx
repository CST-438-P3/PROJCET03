import { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { adminStyles } from './adminStyles';
import { ApiError } from './api';
import { color, font } from './theme';
import { ShelterInput } from './types';

type Field = keyof ShelterInput;

const EMPTY: ShelterInput = { name: '', address: '', zip: '', phone: '', email: '' };
const SAVE_ERROR = "We couldn't save the shelter. Please try again.";

type ShelterFormSheetProps = {
  title: string;
  submitLabel: string;
  // Filled in when editing a shelter, empty when creating one.
  initial?: ShelterInput;
  // Saves the shelter. Throws ApiError when the API says no.
  onSubmit: (input: ShelterInput) => Promise<void>;
  onClose: () => void;
};

/**
 * Splits the API's 400 detail ("ZIP code must be 5 digits; Phone or email is required") into a message per field,
 * so each one shows under its own box. Anything that fits no field goes under "form".
 */
export function fieldErrors(detail: string): Partial<Record<Field | 'form', string>> {
  const errors: Partial<Record<Field | 'form', string>> = {};
  for (const message of detail.split('; ')) {
    let field: Field | 'form' = 'form';
    if (message.startsWith('Name')) field = 'name';
    else if (message.startsWith('Address')) field = 'address';
    else if (message.startsWith('ZIP code') || message.startsWith("We don't know ZIP code")) field = 'zip';
    else if (message.startsWith('Phone')) field = 'phone';
    else if (message.startsWith('Email')) field = 'email';
    errors[field] = errors[field] ? `${errors[field]}. ${message}` : message;
  }
  return errors;
}

/** A sheet that slides up from the bottom with the shelter form, for create and edit (S10). */
export function ShelterFormSheet({ title, submitLabel, initial = EMPTY, onSubmit, onClose }: ShelterFormSheetProps) {
  const [input, setInput] = useState<ShelterInput>(initial);
  const [errors, setErrors] = useState<Partial<Record<Field | 'form', string>>>({});
  const [focused, setFocused] = useState<Field | null>(null);
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true);
    setErrors({});
    try {
      await onSubmit(input);
    } catch (e) {
      const detail = e instanceof ApiError && e.problem.status === 400 ? e.problem.detail : undefined;
      setErrors(detail ? fieldErrors(detail) : { form: SAVE_ERROR });
      setSaving(false);
    }
  }

  function box(field: Field, label: string, placeholder: string, keyboard: 'default' | 'number-pad' | 'phone-pad' | 'email-address' = 'default') {
    const error = errors[field];
    return (
      <View style={styles.field}>
        <Text style={styles.label}>{label}</Text>
        <TextInput
          accessibilityLabel={label}
          value={input[field]}
          onChangeText={(text) => setInput({ ...input, [field]: text })}
          onFocus={() => setFocused(field)}
          onBlur={() => setFocused(null)}
          placeholder={placeholder}
          placeholderTextColor={color.muted}
          keyboardType={keyboard}
          autoCapitalize={field === 'name' || field === 'address' ? 'words' : 'none'}
          maxLength={field === 'zip' ? 5 : undefined}
          style={[adminStyles.input, focused === field && adminStyles.inputFocused, !!error && adminStyles.inputError]}
        />
        {!!error && <Text style={adminStyles.errorText}>{error}</Text>}
      </View>
    );
  }

  return (
    <Modal visible transparent animationType="slide" onRequestClose={onClose} aria-label={title}>
      <View style={styles.dim}>
        {/* Tapping the dimmed area also closes. Screen readers use the Close button instead. */}
        <Pressable aria-hidden focusable={false} style={styles.dimTap} onPress={onClose} />
        <View style={styles.sheet}>
          <View style={styles.grab} />
          <View style={styles.header}>
            <Text role="heading" aria-level={2} style={styles.title}>{title}</Text>
            <Pressable role="button" accessibilityLabel="Close" onPress={onClose} style={styles.close}>
              <Text style={styles.closeText}>✕</Text>
            </Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.form} keyboardShouldPersistTaps="handled">
            {box('name', 'Name', 'Carmel Pet Rescue')}
            {box('address', 'Address', '5 Dolores St')}
            <View style={styles.row}>
              <View style={styles.zip}>{box('zip', 'ZIP code', '93940', 'number-pad')}</View>
              <View style={styles.phone}>{box('phone', 'Phone', '831-555-0000', 'phone-pad')}</View>
            </View>
            {box('email', 'Email', 'name@example.com', 'email-address')}
            <Text style={styles.hint}>Add a phone or an email, so adopters can reach the shelter.</Text>
            {!!errors.form && <Text role="alert" style={styles.formError}>{errors.form}</Text>}
            <Pressable role="button" accessibilityLabel={submitLabel} disabled={saving} onPress={() => void save()} style={styles.submit}>
              {saving ? <ActivityIndicator color={color.surface} /> : <Text style={styles.submitText}>{submitLabel}</Text>}
            </Pressable>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  dim: { flex: 1, backgroundColor: 'rgba(11, 34, 51, 0.35)', justifyContent: 'flex-end' },
  dimTap: { flex: 1 },
  sheet: {
    width: '100%',
    maxWidth: 720,
    maxHeight: '92%',
    alignSelf: 'center',
    backgroundColor: color.surface,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingTop: 10,
  },
  grab: { width: 40, height: 5, borderRadius: 3, backgroundColor: color.line, alignSelf: 'center' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 10 },
  title: { color: color.ink, fontFamily: font.extraBold, fontSize: 24 },
  close: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  closeText: { color: color.muted, fontSize: 20 },
  form: { padding: 20, paddingTop: 8, gap: 14 },
  field: { gap: 6 },
  label: { color: color.muted, fontFamily: font.semiBold, fontSize: 13 },
  row: { flexDirection: 'row', gap: 10 },
  zip: { flex: 0.8 },
  phone: { flex: 1.2 },
  hint: { color: color.muted, fontFamily: font.regular, fontSize: 13 },
  formError: { color: color.danger, fontFamily: font.semiBold, fontSize: 14 },
  submit: { minHeight: 52, borderRadius: 26, backgroundColor: color.ink, alignItems: 'center', justifyContent: 'center' },
  submitText: { color: color.surface, fontFamily: font.semiBold, fontSize: 16 },
});

