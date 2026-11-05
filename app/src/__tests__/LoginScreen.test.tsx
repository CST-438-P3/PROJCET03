import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { ApiError } from '../api';
import { LoginScreen } from '../LoginScreen';

const button = (name: string) => screen.getByRole('button', { name });

describe('LoginScreen', () => {
  it('signs in with the chosen provider and blocks both buttons meanwhile', async () => {
    let finish = () => {};
    const onSignIn = jest.fn(() => new Promise<void>((resolve) => { finish = resolve; }));
    await render(<LoginScreen onSignIn={onSignIn} />);

    await fireEvent.press(button('Sign in with Google'));

    expect(onSignIn).toHaveBeenCalledWith('google');
    expect(button('Opening Google…')).toBeDisabled();
    expect(button('Sign in with GitHub')).toBeDisabled();

    finish();
    await waitFor(() => expect(button('Sign in with Google')).toBeEnabled());
  });

  it("shows the API's message, as for an email registered with the other provider", async () => {
    const detail = 'This email is registered with GitHub';
    const onSignIn = jest.fn().mockRejectedValue(new ApiError({ status: 409, detail }));
    await render(<LoginScreen onSignIn={onSignIn} />);

    await fireEvent.press(button('Sign in with Google'));

    expect(await screen.findByRole('alert')).toHaveTextContent(detail);
    expect(button('Sign in with Google')).toBeEnabled();
  });

  it('shows a generic message for unexpected failures and clears it on retry', async () => {
    const onSignIn = jest.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValue(undefined);
    await render(<LoginScreen onSignIn={onSignIn} />);

    await fireEvent.press(button('Sign in with GitHub'));
    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't sign you in. Please try again.");

    await fireEvent.press(button('Sign in with GitHub'));
    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
  });
});
