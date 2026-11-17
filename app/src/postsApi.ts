import { api } from './api';
import { Post } from './types';

/** Gets one post with its shelter (GET /posts/{postId}, S6). */
export function getPost(postId: number): Promise<Post> {
  return api<Post>(`/posts/${postId}`);
}
