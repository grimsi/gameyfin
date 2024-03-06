import {useAuth} from "Frontend/util/auth";
import {useState} from "react";
import {useNavigate} from "react-router-dom";
import {Button, Card, Input, Typography} from "@material-tailwind/react";

export default function LoginView() {
    const {state, login} = useAuth();
    const [hasError, setError] = useState<boolean>();
    const [username, setUsername] = useState<string>();
    const [password, setPassword] = useState<string>();
    const [url, setUrl] = useState<string>();
    const navigate = useNavigate();

    if (state.user && url) {
        const path = new URL(url, document.baseURI).pathname;
        navigate(path, {replace: true});
    }

    return (
        <div className="flex h-screen">
            <div className="fixed h-full w-full bg-gradient-to-br from-gf-primary to-gf-secondary"></div>
            <Card className="m-auto p-12" shadow={true}>
                <img
                    className="h-28 w-full content-center"
                    src="/images/Logo.svg"
                />
                <div className="mt-8 mb-2 w-80 max-w-screen-lg sm:w-96">
                    <form
                        className="mb-1 flex flex-col gap-6"
                        onSubmit={async e => {
                            e.preventDefault();
                            if (typeof username === "string" && password != null) {
                                const {defaultUrl, error, redirectUrl} = await login(username, password);
                                if (error) {
                                    setError(true);
                                    alert("Wrong username and/or password!");
                                } else {
                                    setUrl(redirectUrl ?? defaultUrl ?? '/');
                                }
                            }
                        }}
                    >
                        <Typography variant="h6" color="blue-gray" className="-mb-3">
                            Username
                        </Typography>
                        <Input
                            onChange={(event) => {
                                setUsername(event.target.value);
                            }}
                            size="lg"
                            className=" !border-t-blue-gray-200 focus:!border-t-gray-900"
                            labelProps={{
                                className: "before:content-none after:content-none",
                            }}
                            crossOrigin="" //TODO: see https://github.com/creativetimofficial/material-tailwind/issues/427
                        />
                        <Typography variant="h6" color="blue-gray" className="-mb-3">
                            Password
                        </Typography>
                        <Input
                            onChange={(event) => {
                                setPassword(event.target.value);
                            }}
                            type="password"
                            size="lg"
                            className=" !border-t-blue-gray-200 focus:!border-t-gray-900"
                            labelProps={{
                                className: "before:content-none after:content-none",
                            }}
                            crossOrigin="" //TODO: see https://github.com/creativetimofficial/material-tailwind/issues/427
                        />
                        <Button
                            type="submit"
                            size="lg"
                            fullWidth>
                            Log in
                        </Button>
                    </form>
                </div>
            </Card>
        </div>
    );
}